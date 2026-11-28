package io.github.dot166.nightstand

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon as ComposeIcon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.dot166.jlib.compose.JLibAppTheme
import io.github.dot166.nightstand.Utils.addMinuteCallback
import io.github.dot166.nightstand.Utils.dimClockView
import io.github.dot166.nightstand.Utils.enforceMainLooper
import io.github.dot166.nightstand.Utils.getAlphaAnimator
import io.github.dot166.nightstand.Utils.getScaleAnimator
import io.github.dot166.nightstand.Utils.removePeriodicCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.min

/**
 * This runnable chooses a random initial position for [.mSaverView] within
 * [.mContentView] if [.mSaverView] is transparent. It also schedules itself to run
 * each minute, at which time [.mSaverView] is faded out, set to a new random location, and
 * faded in.
 */
class MoveScreensaverRunnable(
    /** The container that houses [.mSaverView].  */
    private val mContentView: View,
    /** The display within the [.mContentView] that is randomly positioned.  */
    private val mSaverView: View,
    private val stand: NightStand
) : Runnable {

    /** Accelerate the hide animation.  */
    private val mAcceleration: Interpolator = AccelerateInterpolator()

    /** Decelerate the show animation.  */
    private val mDeceleration: Interpolator = DecelerateInterpolator()

    /** Tracks the currently executing animation if any; used to gracefully stop the animation.  */
    private var mActiveAnimator: Animator? = null

    private val mCalendarModel: CalendarModel = CalendarModel(stand)
    val prefs by nightStandPreferenceStore
    private val clockColorRefresh = MutableStateFlow(0)

    /**
     * Start or restart the random movement of the saver view within the content view.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun start() {
        // Stop any existing animations or callbacks.
        stop()

        // Reset the alpha to 0 so saver view will be randomly positioned within the new bounds.
        mSaverView.setAlpha(0f)

        mCalendarModel.startPolling()
        val mediaFlow = callbackFlow {
            val mediaListener = MediaListener(stand) {
                trySend(getMediaInfo(it))
            }
            mediaListener.onResume()
            awaitClose { mediaListener.onPause() }
        }
        mSaverView.findViewById<ComposeView>(R.id.compose).setContentCompat {
            val events by mCalendarModel.events.collectAsStateWithLifecycle()
            val mediaInfo by mediaFlow.collectAsStateWithLifecycle(null)
            val nightModeEnabled by prefs.nightMode.screensaverNightMode.asFlow()
                .collectAsStateWithLifecycle()
            val nightModeDnd by prefs.nightMode.screensaverNightModeDnd.asFlow()
                .collectAsStateWithLifecycle()
            val dayColor by prefs.clock.screensaverClockColor.asFlow()
                .collectAsStateWithLifecycle()
            val nightColor by prefs.nightMode.screensaverClockNightModeColor.asFlow()
                .collectAsStateWithLifecycle()
            val brightness by prefs.nightMode.screensaverNightModeBrightness.asFlow()
                .collectAsStateWithLifecycle()
            val colorRefresh by clockColorRefresh.collectAsStateWithLifecycle()
            val contentColor = remember(
                nightModeEnabled,
                nightModeDnd,
                dayColor,
                nightColor,
                brightness,
                colorRefresh,
            ) {
                Color(Utils.getClockColorFilter(nightModeEnabled, mSaverView).toColorInt())
            }
            JLibAppTheme {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column {
                        if (events.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_events_found_in_android_system)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(events, key = { it.id }) { event ->
                                    Text(event.toString())
                                }
                            }
                        }

                        mediaInfo?.let { info ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val ctx = LocalContext.current
                                val iconBitmap = remember(info.icon) {
                                    info.icon.loadDrawable(ctx)?.toBitmap()?.asImageBitmap()
                                }
                                if (iconBitmap != null) {
                                    ComposeIcon(
                                        bitmap = iconBitmap,
                                        contentDescription = null,
                                    )
                                }
                                Text(
                                    text = info.title.toString()
                                )
                            }
                            Text(
                                text = info.subtitle.toString()
                            )
                        }
                    }
                }
            }
        }

        // Execute the position updater runnable to choose the first random position of saver view.
        run()

        // Schedule callbacks every minute to adjust the position of mSaverView.
        addMinuteCallback(this, -FADE_TIME)
    }

    data class MediaInfo(val title: CharSequence, val subtitle: CharSequence, val icon: Icon)

    fun getMediaInfo(media: MediaListener): MediaInfo? {
        val tracking = media.tracking ?: return null
        val title = tracking.info.title ?: return null

        val sbn = tracking.sbn
        val icon = sbn.notification.smallIcon ?: Icon.createWithResource(stand, R.drawable.ic_music_note)

        val mediaInfo = tracking.info
        val subtitle = mediaInfo.artist?.takeIf { it.isNotEmpty() }
            ?: sbn?.getAppName(stand)
            ?: stand.getAppName(tracking.packageName)
        return MediaInfo(
            icon = icon,
            title = title,
            subtitle = subtitle,
        )
    }

    // tmp function for interop
    fun ComposeView.setContentCompat(content: @Composable () -> Unit) {
        // Set composition strategy
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        // Inject dependencies normally added by appcompat activities
        setViewTreeLifecycleOwner(stand)
        setViewTreeViewModelStoreOwner(stand)
        setViewTreeSavedStateRegistryOwner(stand)

        // Set content composable
        setContent(content)
    }

    /**
     * Stop the random movement of the saver view within the content view.
     */
    fun stop() {
        removePeriodicCallback(this)

        // End any animation currently running.
        if (mActiveAnimator != null) {
            mActiveAnimator!!.end()
            mActiveAnimator = null
        }
        mCalendarModel.stopPolling()
    }

    private fun dimClockViews(dim: Boolean) {
        dimClockView(dim, mSaverView.findViewById(R.id.digital_clock))
        dimClockView(dim, mSaverView.findViewById(R.id.date_container))
        clockColorRefresh.value++
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun run() {
        enforceMainLooper()

        val selectInitialPosition = mSaverView.alpha == 0f
        if (selectInitialPosition) {
            // When selecting an initial position for the saver view the width and height of
            // mContentView are untrustworthy if this was caused by a configuration change. To
            // combat this, we position the mSaverView randomly within the smallest box that is
            // guaranteed to work.
            val smallestDim = min(mContentView.width, mContentView.height)
            val newX: Float = getRandomPoint((smallestDim - mSaverView.width).toFloat())
            val newY: Float = getRandomPoint((smallestDim - mSaverView.height).toFloat())

            mSaverView.x = newX
            mSaverView.y = newY
            mActiveAnimator = getAlphaAnimator(mSaverView, 0f, 1f)
            mActiveAnimator!!.duration = FADE_TIME
            mActiveAnimator!!.interpolator = mDeceleration
            // Add a slight delay to allow DND mode to engage for the call to Utils.dimClockView().
            mActiveAnimator!!.startDelay = START_DELAY.toLong()
            mActiveAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    // Re-dim the display in case DnD has been enabled and we're following it.
                    dimClockViews(prefs.nightMode.screensaverNightMode.get())
                }
            })
        } else {
            // Select a new random position anywhere in mContentView that will fit mSaverView.
            val newX: Float =
                getRandomPoint((mContentView.width - mSaverView.width).toFloat())
            val newY: Float =
                getRandomPoint((mContentView.height - mSaverView.height).toFloat())

            // Fade out and shrink the saver view.
            val hide = AnimatorSet()
            hide.setDuration(FADE_TIME)
            hide.interpolator = mAcceleration
            hide.play(getAlphaAnimator(mSaverView, 1f, 0f))
                .with(getScaleAnimator(mSaverView, 1f, 0.85f))

            // Fade in and grow the saver view after altering its position.
            val show = AnimatorSet()
            show.setDuration(FADE_TIME)
            // Add a slight delay to allow DND mode to engage for the call to Utils.dimClockView().
            show.setStartDelay(START_DELAY.toLong())
            show.interpolator = mDeceleration
            show.play(getAlphaAnimator(mSaverView, 0f, 1f))
                .with(getScaleAnimator(mSaverView, 0.85f, 1f))
            show.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    mSaverView.x = newX
                    mSaverView.y = newY
                    // Re-dim the display in case DnD has been enabled and we're following it.
                    dimClockViews(prefs.nightMode.screensaverNightMode.get())
                }
            })

            // Execute hide followed by show.
            val all = AnimatorSet()
            all.play(show).after(hide)
            mActiveAnimator = all
        }
        mActiveAnimator!!.start()
    }

    companion object {
        /** The duration over which the fade in/out animations occur.  */
        private const val FADE_TIME = 3000L

        /** The duration (in ms) to delay the start of the fade in animation to allow Do Not Disturb
         * mode to activate.
         */
        private const val START_DELAY = 5

        /**
         * @return a random integer between 0 and the `maximum` exclusive.
         */
        private fun getRandomPoint(maximum: Float): Float {
            return (Math.random() * maximum).toInt().toFloat()
        }
    }
}
