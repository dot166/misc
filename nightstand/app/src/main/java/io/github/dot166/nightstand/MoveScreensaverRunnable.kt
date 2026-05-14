package io.github.dot166.nightstand

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.AdaptiveIconDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.dot166.nightstand.Utils.addMinuteCallback
import io.github.dot166.nightstand.Utils.dimClockView
import io.github.dot166.nightstand.Utils.enforceMainLooper
import io.github.dot166.nightstand.Utils.getAlphaAnimator
import io.github.dot166.nightstand.Utils.getScaleAnimator
import io.github.dot166.nightstand.Utils.removePeriodicCallback
import io.github.dot166.nightstand.Utils.screensaverNightModeOn
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
    private val mSaverView: View
) : Runnable {
    private val mMusicView: View = mSaverView.findViewById(R.id.music_holder)
    private val mRecycler = mSaverView.findViewById<RecyclerView>(R.id.calendar_events)

    /** Accelerate the hide animation.  */
    private val mAcceleration: Interpolator = AccelerateInterpolator()

    /** Decelerate the show animation.  */
    private val mDeceleration: Interpolator = DecelerateInterpolator()

    /** Tracks the currently executing animation if any; used to gracefully stop the animation.  */
    private var mActiveAnimator: Animator? = null

    private val mCalendarModel: CalendarModel = CalendarModel(mContentView.context)

    private lateinit var mediaCallback: MediaController.Callback

    private var currentController: MediaController? = null
    var mIsPlaying = false
    lateinit var mAdapter: EventAdapter

    /**
     * Start or restart the random movement of the saver view within the content view.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun start() {
        // Stop any existing animations or callbacks.
        stop()

        mediaCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateMetadata(metadata)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                mIsPlaying = state!!.state == PlaybackState.STATE_PLAYING
            }
        }

        // Reset the alpha to 0 so saver view will be randomly positioned within the new bounds.
        mSaverView.setAlpha(0f)

        mCalendarModel.startPolling()

        mRecycler.setLayoutManager(LinearLayoutManager(mRecycler.context))
        mRecycler.setItemAnimator(DefaultItemAnimator())
        mRecycler.setHasFixedSize(false)
        mAdapter = EventAdapter(mCalendarModel.events)
        mRecycler.setAdapter(mAdapter)
        mAdapter.notifyDataSetChanged()

        val mediaSessionManager =
            mContentView.context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        val componentName = ComponentName(mContentView.context, NightStandNotificationListener::class.java)

        val controllers = mediaSessionManager.getActiveSessions(componentName)

        val playing = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        if (playing != null) {
            mIsPlaying = true
            currentController = playing
            updateMetadata(playing.metadata)
            playing.registerCallback(mediaCallback)
        }

        mediaSessionManager.addOnActiveSessionsChangedListener(
            { controllers ->
                handleControllersChanged(controllers)
            },
            componentName
        )

        // Execute the position updater runnable to choose the first random position of saver view.
        run()

        // Schedule callbacks every minute to adjust the position of mSaverView.
        addMinuteCallback(this, -FADE_TIME)
    }

    private fun handleControllersChanged(controllers: List<MediaController>?) {
        val playing = controllers!!.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        if (playing != currentController) {
            if (currentController != null) {
                currentController?.unregisterCallback(mediaCallback)
            }
            currentController = playing
            currentController?.registerCallback(mediaCallback)
            mIsPlaying = if (currentController!!.playbackState != null) {
                currentController!!.playbackState!!.state == PlaybackState.STATE_PLAYING
            } else {
                // assume not playing
                false
            }
            updateMetadata(playing?.metadata)
        }
    }

    fun updateMetadata(metadata: MediaMetadata?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)

        val titleView = mMusicView.findViewById<TextView>(R.id.song_title)
        val artistView = mMusicView.findViewById<TextView>(R.id.song_artist)

        titleView.text = title
        artistView.text = artist

        if (currentController?.packageName != null) {
            applyMonochromeAppIcon(titleView, currentController!!.packageName)
        }
    }

    private fun applyMonochromeAppIcon(titleView: TextView, packageName: String) {
        val context = titleView.context
        val pm = context.packageManager

        try {
            val rawIcon = pm.getApplicationIcon(packageName)

            // Extract the best possible layer
            val icon = when {
                Build.VERSION.SDK_INT >= 33 &&
                        rawIcon is AdaptiveIconDrawable &&
                        rawIcon.monochrome != null -> {
                    rawIcon.monochrome!!.mutate()
                }

                rawIcon is AdaptiveIconDrawable -> {
                    rawIcon.foreground.mutate()
                }

                else -> {
                    rawIcon.mutate()
                }
            }

            // Wrap to ensure safe tinting
            val wrapped = DrawableCompat.wrap(icon)

            // Tint to match text color
            DrawableCompat.setTint(wrapped, titleView.currentTextColor)
            DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.SRC_IN)

            // Resize to match line height
            val size = titleView.lineHeight
            wrapped.setBounds(0, 0, size, size)

            titleView.setCompoundDrawablesRelative(
                wrapped,
                null,
                null,
                null
            )

        } catch (e: Exception) {
            // Fallback: clear icon if anything goes wrong
            e.printStackTrace() // we cant use ErrorUtils here as the dialog would close the screensaver
            titleView.setCompoundDrawablesRelative(null, null, null, null)
        }
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

    @SuppressLint("NotifyDataSetChanged")
    override fun run() {
        enforceMainLooper()
        mAdapter = EventAdapter(mCalendarModel.events)
        mRecycler.setAdapter(mAdapter)
        mAdapter.notifyDataSetChanged()
        if (mIsPlaying) {
            mMusicView.visibility = View.VISIBLE
        } else {
            mMusicView.visibility = View.GONE
        }

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
                    dimClockView(screensaverNightModeOn, mSaverView)
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
                    dimClockView(screensaverNightModeOn, mSaverView)
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
