package io.github.dot166.nightstand

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.TextClock
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {
    @JvmStatic
    fun enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw IllegalAccessError("May only call from main thread.")
        }
    }

    /**
     * For screensavers to dim the lights and change the clock color if necessary.
     */
    @JvmStatic
    fun dimClockView(dim: Boolean, clockView: View) {
        val colorFilter = getClockColorFilter(dim, clockView)
        val paint = Paint()
        paint.setColor(Color.WHITE)
        paint.setColorFilter(
            PorterDuffColorFilter(
                colorFilter.toColorInt(),
                PorterDuff.Mode.MULTIPLY
            )
        )
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }

    /**
     * Calculate the color filter to use to dim/color the screensaver display.
     */
    fun getClockColorFilter(dim: Boolean, clockView: View): String {
        var dim = dim
        val nightModeDND: Boolean =
            screensaverNightModeDndOn

        if (nightModeDND) {
            val mNotificationManager = clockView.context
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val filterState = mNotificationManager.getCurrentInterruptionFilter()

            // 0 = INTERRUPTION_FILTER_UNKNOWN
            // 1 = INTERRUPTION_FILTER_ALL (all notifications pass)
            // 2 = INTERRUPTION_FILTER_PRIORITY
            // 3 = INTERRUPTION_FILTER_NONE (no notification passes)
            // 4 = INTERRUPTION_FILTER_ALARMS
            dim = filterState > 1
        }

        val brightnessPercentage: Int =
            screensaverNightModeBrightness
        var colorFilter: String = screensaverClockColor
        if (dim) {
            // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
            val alpha = String.format("%02X", 16 + (176 * brightnessPercentage / 100))
            colorFilter = screensaverClockNightModeColor
            colorFilter = "#$alpha$colorFilter"
        } else {
            colorFilter = "#C0$colorFilter"
        }

        return colorFilter
    }

    /**
     * @return The next alarm from [AlarmManager]
     */
    fun getNextAlarm(context: Context): String? {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val info = am.nextAlarmClock
        if (info != null) {
            val triggerTime = info.triggerTime
            val alarmTime = Calendar.getInstance()
            alarmTime.setTimeInMillis(triggerTime)
            val skeleton = if (DateFormat.is24HourFormat(context)) "EHm" else "Ehma"
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
            return DateFormat.format(pattern, alarmTime) as String?
        }

        return null
    }

    /**
     * Clock views can call this to refresh their alarm to the next upcoming value.
     */
    fun refreshAlarm(context: Context, clock: View?) {
        val nextAlarmIconView = clock!!.findViewById<TextView>(R.id.nextAlarmIcon)
        val nextAlarmView = clock!!.findViewById<TextView?>(R.id.nextAlarm) ?: return

        val alarm = getNextAlarm(context)
        if (!TextUtils.isEmpty(alarm)) {
            val description = context.getString(R.string.next_alarm_description, alarm)
            nextAlarmView.text = alarm
            nextAlarmView.setContentDescription(description)
            nextAlarmView.visibility = View.VISIBLE
            nextAlarmIconView.visibility = View.VISIBLE
            nextAlarmIconView.setContentDescription(description)
        } else {
            nextAlarmView.visibility = View.GONE
            nextAlarmIconView.visibility = View.GONE
        }
    }

    fun setClockIconTypeface(clock: View?) {
        val nextAlarmIconView = clock!!.findViewById<TextView>(R.id.nextAlarmIcon)
        nextAlarmIconView.setTypeface(getAlarmIconTypeface(clock.context))
    }

    /**
     * Clock views can call this to refresh their date.
     */
    fun updateDate(dateSkeleton: String?, descriptionSkeleton: String?, clock: View?) {
        val dateDisplay = clock!!.findViewById<TextView?>(R.id.date) ?: return

        val l = Locale.getDefault()
        val datePattern = DateFormat.getBestDateTimePattern(l, dateSkeleton)
        val descriptionPattern = DateFormat.getBestDateTimePattern(l, descriptionSkeleton)

        val now = Date()
        dateDisplay.text = SimpleDateFormat(datePattern, l).format(now)
        dateDisplay.visibility = View.VISIBLE
        dateDisplay.setContentDescription(SimpleDateFormat(descriptionPattern, l).format(now))
    }

    /***
     * Formats the time in the TextClock for the screensaver according to the Locale with a special
     * formatting treatment for the am/pm label.
     *
     * @param clock          TextClock to format
     * @param includeSeconds whether or not to include seconds in the clock's time
     */
    fun setScreensaverTimeFormat(clock: TextClock?, includeSeconds: Boolean) {
        if (clock != null) {
            val boldText: Boolean = screensaverBoldTextOn
            val showAmPm: Boolean = screensaverShowAmPmOn
            // Get the best format for 12 hours mode according to the locale
            val pattern = get12ModeFormat(
                0.4f,  /* amPmRatio */includeSeconds, boldText,
                showAmPm
            )
            val sp: Spannable = SpannableString(pattern)
            if (boldText) {
                sp.setSpan(
                    StyleSpan(Typeface.BOLD), 0, pattern.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            clock.setFormat12Hour(sp)
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(includeSeconds))
        }
    }

    /**
     * @param amPmRatio      a value between 0 and 1 that is the ratio of the relative size of the
     * am/pm string to the time string
     * @param includeSeconds whether or not to include seconds in the time string
     * @param amPmBolded     whether or not to bold the AM/PM
     * @param amPmDisplayed  whether or not to show the AM/PM
     * @return format string for 12 hours mode time, not including seconds
     */
    fun get12ModeFormat(
        amPmRatio: Float, includeSeconds: Boolean,
        amPmBolded: Boolean, amPmDisplayed: Boolean
    ): CharSequence {
        var pattern = DateFormat.getBestDateTimePattern(
            Locale.getDefault(),
            if (includeSeconds) "hmsa" else "hma"
        )
        if (amPmRatio <= 0 || !amPmDisplayed) {
            pattern = pattern.replace("a".toRegex(), "").trim { it <= ' ' }
        }

        // Replace spaces with "Hair Space"
        pattern = pattern.replace(" ".toRegex(), "\u200A")
        // Build a spannable so that the am/pm will be formatted
        val amPmPos = pattern.indexOf('a')
        if (amPmPos == -1) {
            return pattern
        }

        val sp: Spannable = SpannableString(pattern)
        sp.setSpan(
            RelativeSizeSpan(amPmRatio), amPmPos, amPmPos + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sp.setSpan(
            StyleSpan(if (amPmBolded) Typeface.BOLD else Typeface.NORMAL), amPmPos,
            amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sp.setSpan(
            TypefaceSpan("sans-serif"), amPmPos, amPmPos + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return sp
    }

    fun get24ModeFormat(includeSeconds: Boolean): CharSequence {
        return DateFormat.getBestDateTimePattern(
            Locale.getDefault(),
            if (includeSeconds) "Hms" else "Hm"
        )
    }

    /** The model from which timed callbacks originate.  */
    private var mPeriodicCallbackModel: PeriodicCallbackModel? = null

    private var mPrefs: Settings? = null

    /**
     * To display the alarm clock in this font, use the character [R.string.clock_emoji].
     *
     * @return a special font containing a glyph that draws an alarm clock
     */
    fun getAlarmIconTypeface(context: Context): Typeface? {
        return Typeface.createFromAsset(context.assets, "fonts/clock.ttf")
    }

    fun init(ctx: Context) {
        val dp = ctx.createDeviceProtectedStorageContext()
        mPrefs = Settings(
            PreferenceManager.getDefaultSharedPreferences(dp),
            dp.getString(R.string.default_clock_color)
        )
        mPeriodicCallbackModel = PeriodicCallbackModel(dp)
    }

    /**
     * @param runnable to be called every minute
     * @param offset an offset applied to the minute to control when the callback occurs
     */
    @JvmStatic
    fun addMinuteCallback(runnable: Runnable, offset: Long) {
        enforceMainLooper()
        mPeriodicCallbackModel!!.addMinuteCallback(runnable, offset)
    }

    /**
     * @param runnable to be called every midnight
     */
    fun addMidnightCallback(runnable: Runnable) {
        enforceMainLooper()
        mPeriodicCallbackModel!!.addMidnightCallback(runnable)
    }

    /**
     * @param runnable to no longer be called periodically
     */
    @JvmStatic
    fun removePeriodicCallback(runnable: Runnable?) {
        enforceMainLooper()
        mPeriodicCallbackModel!!.removePeriodicCallback(runnable)
    }

    val screensaverClockColor: String
        get() = mPrefs!!.getScreensaverClockColor()

    val screensaverClockNightModeColor: String
        get() = mPrefs!!.getClockNightModeColor()

    val screensaverNightModeDndOn: Boolean
        get() = mPrefs!!.getScreensaverNightModeDndOn()

    val screensaverNightModeBrightness: Int
        get() = mPrefs!!.getScreensaverNightModeBrightness()

    @JvmStatic
    val screensaverNightModeOn: Boolean
        get() = mPrefs!!.getScreensaverNightModeOn()

    val screensaverShowAmPmOn: Boolean
        get() = mPrefs!!.getScreensaverShowAmPmOn()

    val screensaverBoldTextOn: Boolean
        get() = mPrefs!!.getScreensaverBoldTextOn()

    @JvmStatic
    fun getScaleAnimator(view: View?, vararg values: Float): ValueAnimator {
        return ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, *values),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, *values)
        )
    }

    @JvmStatic
    fun getAlphaAnimator(view: View?, vararg values: Float): ValueAnimator? {
        return ObjectAnimator.ofFloat(view, View.ALPHA, *values)
    }
}
