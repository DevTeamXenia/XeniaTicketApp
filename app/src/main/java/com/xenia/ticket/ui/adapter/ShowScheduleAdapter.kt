package com.xenia.ticket.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import com.xenia.ticket.data.network.model.ShowScheduleResponse
import com.xenia.ticket.utils.common.CommonMethod.formatTime

class ShowScheduleAdapter(
    private var list: List<ShowScheduleResponse>,
    private val onClick: (ShowScheduleResponse) -> Unit
) : RecyclerView.Adapter<ShowScheduleAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTime: TextView = view.findViewById(R.id.txtTime)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    val previous = selectedPosition
                    selectedPosition = position

                    notifyItemChanged(previous)
                    notifyItemChanged(selectedPosition)

                    onClick(list[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_show_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val start = formatTime(item.StartTime)
        val end = formatTime(item.EndTime)

        holder.txtTime.text = "$start - $end"

        if (position == selectedPosition) {
            holder.txtTime.setBackgroundResource(R.drawable.bg_selected)
            holder.txtTime.setTextColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.txtTime.setBackgroundResource(R.drawable.edittext_bg)
            holder.txtTime.setTextColor(holder.itemView.context.getColor(R.color.black))
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<ShowScheduleResponse>) {
        list = newList
        selectedPosition = if (newList.isNotEmpty()) 0 else -1
        notifyDataSetChanged()
    }

    // ✅ Restore selection
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedByScheduleId(scheduleId: Int) {
        val index = list.indexOfFirst { it.ScheduleId == scheduleId }
        if (index != -1) {
            selectedPosition = index
            notifyDataSetChanged()
        }
    }
}

