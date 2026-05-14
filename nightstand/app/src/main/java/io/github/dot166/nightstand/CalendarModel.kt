package io.github.dot166.nightstand

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.core.os.ConfigurationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class CalendarModel(val ctx: Context) {

    var events: List<Event> = listOf()

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    private val mCalendarPoll: Runnable = object : Runnable {
        override fun run() {
            events = fetch()
            mHandler.postDelayed(this, 15 * 60 * 1000L)
        }
    }

    data class Event(val title: String, val start: Long, val end: Long, val allDay: Boolean, val ctx: Context) {
        override fun toString(): String {
            val cal = Calendar.getInstance().apply {
                timeInMillis = start
            }

            val endCal = Calendar.getInstance().apply {
                timeInMillis = end
            }

            val currentLocale: Locale = ConfigurationCompat.getLocales(ctx.resources.configuration)[0]!!
            val fmt = SimpleDateFormat(DateFormat.getBestDateTimePattern(currentLocale, "HH:mm:ss dd-MM-yyyy"), currentLocale)
            val durationStr: String = if (allDay) {
                ctx.getString(R.string.all_day)
            } else {
                ctx.getString(R.string.until, fmt.format(endCal.time))
            }
            return ctx.getString(
                R.string.event_str_main,
                title,
                fmt.format(cal.time),
                durationStr
            )
        }
    }

    fun fetch(): List<Event> {
        val list = mutableListOf<Event>()
        val now = System.currentTimeMillis()
        val oneWeekLater = now + DateUtils.WEEK_IN_MILLIS
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .also { builder ->
                ContentUris.appendId(builder, now)
                ContentUris.appendId(builder, oneWeekLater)
            }
            .build()
        val projection = arrayOf<String?>(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )
        val selection =
            CalendarContract.Instances.VISIBLE + " = 1 AND " +
                    CalendarContract.Instances.SELF_ATTENDEE_STATUS + " != " +
                    CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
        val sortOrder = CalendarContract.Instances.BEGIN + " ASC"
        val c: Cursor? = ctx.contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )
        if (c != null) {
            for (i in 0 until min(c.count, 5)) {
                c.moveToPosition(i)
                val title = c.getString(0)
                val start = c.getLong(1)
                val end = c.getLong(2)
                val allDay = c.getInt(3) != 0
                list.add(Event(title, start, end, allDay, ctx))
            }
        }
        c?.close()
        return list
    }

    fun startPolling() {
        mHandler.post(mCalendarPoll)
    }

    fun stopPolling() {
        mHandler.removeCallbacks(mCalendarPoll)
    }
}