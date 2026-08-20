








package com.xenia.ticket.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
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

    private var selectedPosition = RecyclerView.NO_POSITION

    companion object {
        private const val FEW_SEATS_THRESHOLD = 20
    }

    inner class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val txtTimeLabel: TextView =
            view.findViewById(R.id.txtTimeLabel)

        val txtSeatStatus: TextView =
            view.findViewById(R.id.txtSeatStatus)

        init {

            view.setOnClickListener {

                val position = bindingAdapterPosition

                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                val item = list[position]



                if (item.AvailableSeats <= 0) {
                    return@setOnClickListener
                }

                val previousPosition = selectedPosition

                // Already selected
                if (previousPosition == position) {
                    onClick(item)
                    return@setOnClickListener
                }

                selectedPosition = position

                // Refresh previous selection
                if (previousPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousPosition)
                }

                // Refresh new selection
                notifyItemChanged(selectedPosition)

                // Return selected schedule to Dialog
                onClick(item)
            }
        }
    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_time_chip,
                parent,
                false
            )

        return ViewHolder(view)
    }


    override fun getItemCount(): Int {
        return list.size
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]
        val context = holder.itemView.context

        Log.d(
            "SCHEDULE_ADAPTER",
            "position=$position, scheduleId=${item.ScheduleId}, AvailableSeats=${item.AvailableSeats}"
        )

        holder.txtTimeLabel.text = formatTime(item.StartTime)

        when {
            // 0 = FULL
            item.AvailableSeats == 0 -> {

                holder.itemView.isSelected = false
                holder.itemView.isActivated = false
                holder.itemView.isEnabled = false

                holder.txtSeatStatus.text = "Full"

                holder.txtTimeLabel.setTextColor(
                    context.getColor(R.color.gray)
                )

                holder.txtSeatStatus.setTextColor(
                    context.getColor(R.color.gray)
                )
            }

            // 1 - 9 = FEW SEATS
            item.AvailableSeats in 1..9 -> {

                holder.itemView.isSelected = false
                holder.itemView.isActivated = true
                holder.itemView.isEnabled = true

                holder.txtSeatStatus.text = "Few Seats"

                holder.txtTimeLabel.setTextColor(
                    context.getColor(R.color.black)
                )

                holder.txtSeatStatus.setTextColor(
                    Color.parseColor("#FF9800")
                )
            }

            // SELECTED
            position == selectedPosition -> {

                holder.itemView.isSelected = true
                holder.itemView.isActivated = false
                holder.itemView.isEnabled = true

                holder.txtSeatStatus.text = "Selected"

                holder.txtTimeLabel.setTextColor(
                    context.getColor(R.color.black)
                )

                holder.txtSeatStatus.setTextColor(
                    context.getColor(R.color.black)
                )
            }


            else -> {

                holder.itemView.isSelected = false
                holder.itemView.isActivated = false
                holder.itemView.isEnabled = true

                holder.txtSeatStatus.text =
                    "${item.AvailableSeats} Seats"

                holder.txtTimeLabel.setTextColor(
                    context.getColor(R.color.black)
                )

                holder.txtSeatStatus.setTextColor(
                    context.getColor(R.color.gray)
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<ShowScheduleResponse>) {

        Log.d(
            "SCHEDULE_ADAPTER",
            "updateData() CALLED, size=${newList.size}"
        )

        newList.forEach {
            Log.d(
                "SCHEDULE_ADAPTER",
                "ID=${it.ScheduleId}, " +
                        "TIME=${it.StartTime}, " +
                        "SEATS=${it.AvailableSeats}"
            )
        }

        list = newList

        selectedPosition =
            newList.indexOfFirst {
                it.AvailableSeats > 0
            }

        notifyDataSetChanged()
    }



    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedByScheduleId(
        scheduleId: Int
    ) {

        val index =
            list.indexOfFirst {
                it.ScheduleId == scheduleId &&
                        it.AvailableSeats > 0
            }

        if (index == -1) {
            selectedPosition = RecyclerView.NO_POSITION
            notifyDataSetChanged()
            return
        }

        selectedPosition = index

        notifyDataSetChanged()
    }



    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {

        selectedPosition = RecyclerView.NO_POSITION

        notifyDataSetChanged()
    }



    fun getSelectedSchedule(): ShowScheduleResponse? {

        if (
            selectedPosition == RecyclerView.NO_POSITION ||
            selectedPosition !in list.indices
        ) {
            return null
        }

        val item = list[selectedPosition]

        if (item.AvailableSeats <= 0) {
            return null
        }

        return item
    }



    fun getSelectedScheduleId(): Int? {

        return getSelectedSchedule()?.ScheduleId
    }
}