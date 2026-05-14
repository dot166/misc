package io.github.dot166.nightstand

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(val eventList: List<CalendarModel.Event>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_EMPTY = 0
    private val VIEW_TYPE_EVENT = 1

    override fun getItemViewType(position: Int): Int {
        return if (eventList.isEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row, parent, false)

        return if (viewType == VIEW_TYPE_EMPTY) {
            EmptyViewHolder(v)
        } else {
            EventViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder -> {
                (holder.itemView as TextView).text = eventList[position].toString()
            }

            is EmptyViewHolder -> {
                (holder.itemView as TextView).text =
                    holder.itemView.context.getString(R.string.no_events_found_in_android_system)
            }

            else -> {
                (holder.itemView as TextView).text = "" // should never happen
            }
        }
    }

    override fun getItemCount(): Int {
        return if (eventList.isEmpty()) 1 else eventList.size
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}