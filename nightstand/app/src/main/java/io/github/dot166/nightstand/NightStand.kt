package io.github.dot166.nightstand

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextClock


class NightStand : DreamService() {
    private val mStartPositionUpdater: ViewTreeObserver.OnPreDrawListener = StartPositionUpdater()
    private var mPositionUpdater: MoveScreensaverRunnable? = null

    private var mDateFormat: String? = null
    private var mDateFormatForAccessibility: String? = null

    private var mContentView: View? = null
    private var mMainClockView: View? = null
    private var mDigitalClock: TextClock? = null

    // Runs every midnight or when the time changes and refreshes the date.
    private val mMidnightUpdater: Runnable =
        Runnable { Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView) }

    /**
     * Receiver to alarm clock changes.
     */
    private val mAlarmChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Utils.refreshAlarm(this@NightStand, mContentView)
        }
    }
    private var receiverRegistered = false

    override fun onCreate() {
        Log.i("NightStand", "NightStand created")

        setTheme(com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        super.onCreate()

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year)
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year)
    }

    override fun onAttachedToWindow() {
        Log.i("NightStand", "NightStand attached to window")
        super.onAttachedToWindow()

        setContentView(R.layout.saver)

        mContentView = findViewById(R.id.saver_container)
        mMainClockView = mContentView!!.findViewById(R.id.content)
        mDigitalClock = mMainClockView!!.findViewById(R.id.digital_clock)

        isScreenBright = false
        Utils.setClockIconTypeface(mContentView)
        Utils.setScreensaverTimeFormat(mDigitalClock, true)

        mContentView!!.setSystemUiVisibility(
            (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        )

        mPositionUpdater = MoveScreensaverRunnable(mContentView!!, mMainClockView!!)

        // We want the screen saver to exit upon user interaction.
        isInteractive = false
        isFullscreen = true

        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView)
        Utils.refreshAlarm(this, mContentView)

        startPositionUpdater()
        Utils.addMidnightCallback(mMidnightUpdater)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()

        // Setup handlers for time reference changes and date updates.
        if (!receiverRegistered) {
            registerReceiver(
                mAlarmChangedReceiver,
                IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
                RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onDetachedFromWindow() {
        Log.i("NightStand", "NightStand detached from window")
        super.onDetachedFromWindow()

        Utils.removePeriodicCallback(mMidnightUpdater)
        stopPositionUpdater()
    }

    override fun onDreamingStopped() {
        Log.i("NightStand", "NightStand finished")
        super.onDreamingStopped()

        // Tear down handlers for time reference changes and date updates.
        if (receiverRegistered) {
            unregisterReceiver(mAlarmChangedReceiver)
            receiverRegistered = false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.i("NightStand", "NightStand configuration changed")
        super.onConfigurationChanged(newConfig)

        startPositionUpdater()
    }

    /**
     * The [.mContentView] will be drawn shortly. When that draw occurs, the position updater
     * callback will also be executed to choose a random position for the time display as well as
     * schedule future callbacks to move the time display each minute.
     */
    private fun startPositionUpdater() {
        if (mContentView != null) {
            mContentView!!.getViewTreeObserver().addOnPreDrawListener(mStartPositionUpdater)
        }
    }

    /**
     * This activity is no longer in the foreground; position callbacks should be removed.
     */
    private fun stopPositionUpdater() {
        if (mContentView != null) {
            mContentView!!.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater)
        }
        mPositionUpdater!!.stop()
    }

    private inner class StartPositionUpdater : ViewTreeObserver.OnPreDrawListener {
        /**
         * This callback occurs after initial layout has completed. It is an appropriate place to
         * select a random position for [.mMainClockView] and schedule future callbacks to update
         * its position.
         *
         * @return `true` to continue with the drawing pass
         */
        override fun onPreDraw(): Boolean {
            if (mContentView!!.getViewTreeObserver().isAlive) {
                // (Re)start the periodic position updater.
                mPositionUpdater!!.start()

                // This listener must now be removed to avoid starting the position updater again.
                mContentView!!.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater)
            }
            return true
        }
    }
}