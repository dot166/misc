package io.github.dot166.nightstand

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarModel(val ctx: Context) {

    var event: Event? = null

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    private val mCalendarPoll: Runnable = object : Runnable {
        override fun run() {
            event = fetch()
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

            val fmt = SimpleDateFormat("HH:mm:ss dd-MM-Y", Locale.getDefault());
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

    fun fetch(): Event? {
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
        if (c != null && c.moveToFirst()) {
            val title = c.getString(0)
            val start = c.getLong(1)
            val end = c.getLong(2)
            val allDay = c.getInt(3) != 0

            return Event(title, start, end, allDay, ctx)
        }
        c?.close()
        return null
    }
    fun hasUpcomingEvent(): Boolean {
        return event != null
    }

    fun startPolling() {
        mHandler.post(mCalendarPoll)
    }

    fun stopPolling() {
        mHandler.removeCallbacks(mCalendarPoll)
    }
}