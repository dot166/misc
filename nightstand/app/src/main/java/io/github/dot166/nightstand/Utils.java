package io.github.dot166.nightstand;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    /**
     * For screensavers to dim the lights and change the clock color if necessary.
     */
    public static void dimClockView(boolean dim, View clockView) {
        String colorFilter = getClockColorFilter(dim, clockView);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter(Color.parseColor(colorFilter),
                PorterDuff.Mode.MULTIPLY));
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * Calculate the color filter to use to dim/color the screensaver display.
     */
    public static String getClockColorFilter(boolean dim, View clockView) {
        boolean nightModeDND = getScreensaverNightModeDndOn();

        if (nightModeDND) {
            NotificationManager mNotificationManager = (NotificationManager) clockView.getContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            int filterState = mNotificationManager.getCurrentInterruptionFilter();

            // 0 = INTERRUPTION_FILTER_UNKNOWN
            // 1 = INTERRUPTION_FILTER_ALL (all notifications pass)
            // 2 = INTERRUPTION_FILTER_PRIORITY
            // 3 = INTERRUPTION_FILTER_NONE (no notification passes)
            // 4 = INTERRUPTION_FILTER_ALARMS
            dim = filterState > 1;
        }

        final int brightnessPercentage = getScreensaverNightModeBrightness();
        String colorFilter = getScreensaverClockColor();
        if (dim) {
            // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
            String alpha = String.format("%02X", 16 + (176 * brightnessPercentage / 100));
            colorFilter = getScreensaverClockNightModeColor();
            colorFilter = "#" + alpha + colorFilter;
        } else {
            colorFilter = "#C0" + colorFilter;
        }

        return colorFilter;
    }

    /**
     * @return The next alarm from {@link AlarmManager}
     */
    public static String getNextAlarm(Context context) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final AlarmClockInfo info = getNextAlarmClock(am);
        if (info != null) {
            final long triggerTime = info.getTriggerTime();
            final Calendar alarmTime = Calendar.getInstance();
            alarmTime.setTimeInMillis(triggerTime);
            final String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
            final String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
            return (String) DateFormat.format(pattern, alarmTime);
        }

        return null;
    }

    private static AlarmClockInfo getNextAlarmClock(AlarmManager am) {
        return am.getNextAlarmClock();
    }

    /**
     * Clock views can call this to refresh their alarm to the next upcoming value.
     */
    public static void refreshAlarm(Context context, View clock) {
        final TextView nextAlarmIconView = clock.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarmView = clock.findViewById(R.id.nextAlarm);
        if (nextAlarmView == null) {
            return;
        }

        final String alarm = getNextAlarm(context);
        if (!TextUtils.isEmpty(alarm)) {
            final String description = context.getString(R.string.next_alarm_description, alarm);
            nextAlarmView.setText(alarm);
            nextAlarmView.setContentDescription(description);
            nextAlarmView.setVisibility(View.VISIBLE);
            nextAlarmIconView.setVisibility(View.VISIBLE);
            nextAlarmIconView.setContentDescription(description);
        } else {
            nextAlarmView.setVisibility(View.GONE);
            nextAlarmIconView.setVisibility(View.GONE);
        }
    }

    public static void setClockIconTypeface(View clock) {
        final TextView nextAlarmIconView = clock.findViewById(R.id.nextAlarmIcon);
        nextAlarmIconView.setTypeface(getAlarmIconTypeface(clock.getContext()));
    }

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateDate(String dateSkeleton, String descriptionSkeleton, View clock) {
        final TextView dateDisplay = clock.findViewById(R.id.date);
        if (dateDisplay == null) {
            return;
        }

        final Locale l = Locale.getDefault();
        final String datePattern = DateFormat.getBestDateTimePattern(l, dateSkeleton);
        final String descriptionPattern = DateFormat.getBestDateTimePattern(l, descriptionSkeleton);

        final Date now = new Date();
        dateDisplay.setText(new SimpleDateFormat(datePattern, l).format(now));
        dateDisplay.setVisibility(View.VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, l).format(now));
    }

    /***
     * Formats the time in the TextClock for the screensaver according to the Locale with a special
     * formatting treatment for the am/pm label.
     *
     * @param clock          TextClock to format
     * @param includeSeconds whether or not to include seconds in the clock's time
     */
    public static void setScreensaverTimeFormat(TextClock clock, boolean includeSeconds) {
        if (clock != null) {
            final boolean boldText = getScreensaverBoldTextOn();
            final boolean showAmPm = getScreensaverShowAmPmOn();
            // Get the best format for 12 hours mode according to the locale
            CharSequence pattern = get12ModeFormat(0.4f /* amPmRatio */, includeSeconds, boldText,
                    showAmPm);
            final Spannable sp = new SpannableString(pattern);
            if (boldText) {
                sp.setSpan(new StyleSpan(Typeface.BOLD), 0, pattern.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            clock.setFormat12Hour(sp);
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(includeSeconds));
        }
    }

    /**
     * @param amPmRatio      a value between 0 and 1 that is the ratio of the relative size of the
     *                       am/pm string to the time string
     * @param includeSeconds whether or not to include seconds in the time string
     * @param amPmBolded     whether or not to bold the AM/PM
     * @param amPmDisplayed  whether or not to show the AM/PM
     * @return format string for 12 hours mode time, not including seconds
     */
    public static CharSequence get12ModeFormat(float amPmRatio, boolean includeSeconds,
            boolean amPmBolded, boolean amPmDisplayed) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                includeSeconds ? "hmsa" : "hma");
        if (amPmRatio <= 0 || !amPmDisplayed) {
            pattern = pattern.replaceAll("a", "").trim();
        }

        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll(" ", "\u200A");
        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }

        final Spannable sp = new SpannableString(pattern);
        sp.setSpan(new RelativeSizeSpan(amPmRatio), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StyleSpan(amPmBolded ? Typeface.BOLD : Typeface.NORMAL), amPmPos,
                amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new TypefaceSpan("sans-serif"), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sp;
    }

    public static CharSequence get24ModeFormat(boolean includeSeconds) {
        return DateFormat.getBestDateTimePattern(Locale.getDefault(),
                includeSeconds ? "Hms" : "Hm");
    }

    private static Context mContext;

    /** The model from which timed callbacks originate. */
    private static PeriodicCallbackModel mPeriodicCallbackModel;

    private static SharedPreferences mPrefs;

    /**
     * To display the alarm clock in this font, use the character {@link R.string#clock_emoji}.
     *
     * @return a special font containing a glyph that draws an alarm clock
     */
    public static Typeface getAlarmIconTypeface(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/clock.ttf");
    }

    public static void init(Context ctx) {
        Context dp = ctx.createDeviceProtectedStorageContext();
        mContext = dp;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(dp);
        mPeriodicCallbackModel = new PeriodicCallbackModel(dp);
    }

    /**
     * @param runnable to be called every minute
     * @param offset an offset applied to the minute to control when the callback occurs
     */
    public static void addMinuteCallback(Runnable runnable, long offset) {
        enforceMainLooper();
        mPeriodicCallbackModel.addMinuteCallback(runnable, offset);
    }

    /**
     * @param runnable to be called every quarter-hour
     */
    public static void addQuarterHourCallback(Runnable runnable) {
        enforceMainLooper();
        mPeriodicCallbackModel.addQuarterHourCallback(runnable);
    }

    /**
     * @param runnable to be called every midnight
     */
    public static void addMidnightCallback(Runnable runnable) {
        enforceMainLooper();
        mPeriodicCallbackModel.addMidnightCallback(runnable);
    }

    /**
     * @param runnable to no longer be called periodically
     */
    public static void removePeriodicCallback(Runnable runnable) {
        enforceMainLooper();
        mPeriodicCallbackModel.removePeriodicCallback(runnable);
    }

    static String getScreensaverClockColor() {
        return (new Settings()).getScreensaverClockColor(mContext, mPrefs);
    }

    static String getScreensaverClockNightModeColor() {
        return (new Settings()).getClockNightModeColor(mContext, mPrefs);
    }

    static boolean getScreensaverNightModeDndOn() {
        return (new Settings()).getScreensaverNightModeDndOn(mPrefs);
    }

    static int getScreensaverNightModeBrightness() {
        return (new Settings()).getScreensaverNightModeBrightness(mPrefs);
    }

    static boolean getScreensaverNightModeOn() {
        return (new Settings()).getScreensaverNightModeOn(mPrefs);
    }

    static boolean getScreensaverShowAmPmOn() {
        return (new Settings()).getScreensaverShowAmPmOn(mPrefs);
    }

    static boolean getScreensaverBoldTextOn() {
        return (new Settings()).getScreensaverBoldTextOn(mPrefs);
    }

    public static ValueAnimator getScaleAnimator(View view, float... values) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, values),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, values));
    }

    public static ValueAnimator getAlphaAnimator(View view, float... values) {
        return ObjectAnimator.ofFloat(view, View.ALPHA, values);
    }
}
