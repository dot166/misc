package io.github.dot166.nightstand

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import androidx.annotation.VisibleForTesting
import io.github.dot166.nightstand.Utils.enforceMainLooper
import java.util.Calendar
import java.util.concurrent.CopyOnWriteArrayList

/**
 * All callbacks to be delivered at requested times on the main thread if the application is in the
 * foreground when the callback time passes.
 */
internal class PeriodicCallbackModel(context: Context) {
    @VisibleForTesting
    internal enum class Period {
        MINUTE, QUARTER_HOUR, HOUR, MIDNIGHT
    }

    /** Reschedules callbacks when the device time changes.  */
    private val mTimeChangedReceiver: BroadcastReceiver = TimeChangedReceiver()

    private val mPeriodicRunnables: MutableList<PeriodicRunnable> =
        CopyOnWriteArrayList<PeriodicRunnable>()

    init {
        // Reschedules callbacks when the device time changes.
        val timeChangedBroadcastFilter = IntentFilter()
        timeChangedBroadcastFilter.addAction(Intent.ACTION_TIME_CHANGED)
        timeChangedBroadcastFilter.addAction(Intent.ACTION_DATE_CHANGED)
        timeChangedBroadcastFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        context.registerReceiver(
            mTimeChangedReceiver, timeChangedBroadcastFilter,
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * @param runnable to be called every minute
     * @param offset an offset applied to the minute to control when the callback occurs
     */
    fun addMinuteCallback(runnable: Runnable, offset: Long) {
        addPeriodicCallback(runnable, Period.MINUTE, offset)
    }

    /**
     * @param runnable to be called every quarter-hour
     */
    @Suppress("unused")
    fun addQuarterHourCallback(runnable: Runnable) {
        // Callbacks *can* occur early so pad in an extra 100ms on the quarter-hour callback
        // to ensure the sampled wallclock time reflects the subsequent quarter-hour.
        addPeriodicCallback(runnable, Period.QUARTER_HOUR, 100L)
    }

    /**
     * @param runnable to be called every hour
     */
    @Suppress("unused")
    fun addHourCallback(runnable: Runnable) {
        // Callbacks *can* occur early so pad in an extra 100ms on the hour callback to ensure
        // the sampled wallclock time reflects the subsequent hour.
        addPeriodicCallback(runnable, Period.HOUR, 100L)
    }

    /**
     * @param runnable to be called every midnight
     */
    fun addMidnightCallback(runnable: Runnable) {
        // Callbacks *can* occur early so pad in an extra 100ms on the midnight callback to ensure
        // the sampled wallclock time reflects the subsequent day.
        addPeriodicCallback(runnable, Period.MIDNIGHT, 100L)
    }

    /**
     * @param runnable to be called periodically
     */
    private fun addPeriodicCallback(runnable: Runnable, period: Period, offset: Long) {
        val periodicRunnable = PeriodicRunnable(runnable, period, offset)
        mPeriodicRunnables.add(periodicRunnable)
        periodicRunnable.schedule()
    }

    /**
     * @param runnable to no longer be called periodically
     */
    fun removePeriodicCallback(runnable: Runnable?) {
        for (periodicRunnable in mPeriodicRunnables) {
            if (periodicRunnable.mDelegate === runnable) {
                periodicRunnable.unSchedule()
                mPeriodicRunnables.remove(periodicRunnable)
                return
            }
        }
    }

    /**
     * Schedules the execution of the given delegate Runnable at the next callback time.
     */
    private class PeriodicRunnable(
        val mDelegate: Runnable,
        private val mPeriod: Period,
        private val mOffset: Long
    ) : Runnable {
        override fun run() {
            Log.i(
                "NightStand",
                "Executing periodic callback for %s because the period ended$mPeriod"
            )
            mDelegate.run()
            schedule()
        }

        fun runAndReschedule() {
            Log.i(
                "NightStand",
                "Executing periodic callback for %s because the time changed$mPeriod"
            )
            unSchedule()
            mDelegate.run()
            schedule()
        }

        fun schedule() {
            val delay: Long = getDelay(System.currentTimeMillis(), mPeriod, mOffset)
            handler.postDelayed(this, delay)
        }

        fun unSchedule() {
            handler.removeCallbacks(this)
        }
    }

    /**
     * Reschedules callbacks when the device time changes.
     */
    private inner class TimeChangedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            for (periodicRunnable in mPeriodicRunnables) {
                periodicRunnable.runAndReschedule()
            }
        }
    }

    companion object {
        private const val QUARTER_HOUR_IN_MILLIS = 15 * DateUtils.MINUTE_IN_MILLIS

        private var sHandler: Handler? = null

        /**
         * Return the delay until the given `period` elapses adjusted by the given `offset`.
         *
         * @param now the current time
         * @param period the frequency with which callbacks should be given
         * @param offset an offset to add to the normal period; allows the callback to be made relative
         * to the normally scheduled period end
         * @return the time delay from `now` to schedule the callback
         */
        @VisibleForTesting
        fun getDelay(now: Long, period: Period, offset: Long): Long {
            val periodStart = now - offset

            when (period) {
                Period.MINUTE -> {
                    val lastMinute = periodStart - (periodStart % DateUtils.MINUTE_IN_MILLIS)
                    val nextMinute = lastMinute + DateUtils.MINUTE_IN_MILLIS
                    return nextMinute - now + offset
                }

                Period.QUARTER_HOUR -> {
                    val lastQuarterHour: Long = periodStart - (periodStart % QUARTER_HOUR_IN_MILLIS)
                    val nextQuarterHour: Long = lastQuarterHour + QUARTER_HOUR_IN_MILLIS
                    return nextQuarterHour - now + offset
                }

                Period.HOUR -> {
                    val lastHour = periodStart - (periodStart % DateUtils.HOUR_IN_MILLIS)
                    val nextHour = lastHour + DateUtils.HOUR_IN_MILLIS
                    return nextHour - now + offset
                }

                Period.MIDNIGHT -> {
                    val nextMidnight = Calendar.getInstance()
                    nextMidnight.setTimeInMillis(periodStart)
                    nextMidnight.add(Calendar.DATE, 1)
                    nextMidnight.set(Calendar.HOUR_OF_DAY, 0)
                    nextMidnight.set(Calendar.MINUTE, 0)
                    nextMidnight.set(Calendar.SECOND, 0)
                    nextMidnight.set(Calendar.MILLISECOND, 0)
                    return nextMidnight.getTimeInMillis() - now + offset
                }

            }
        }

        private val handler: Handler
            get() {
                enforceMainLooper()
                if (sHandler == null) {
                    sHandler = Handler(Looper.myLooper()!!)
                }
                return sHandler!!
            }
    }
}
