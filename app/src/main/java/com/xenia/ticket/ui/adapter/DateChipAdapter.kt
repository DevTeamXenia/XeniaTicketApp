package com.xenia.ticket.ui.adapter

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log

class DateChipAdapter(
    private var dates: List<String>, // yyyy-MM-dd
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_CUSTOM = 1
    }

    private var selectedPosition = 0
    private var customDate: String? = null

    private val apiDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    private val displayDateFormat =
        SimpleDateFormat("dd MMM", Locale.getDefault())

    private val customPosition: Int
        get() = dates.size

    inner class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val txtDateLabel: TextView =
            view.findViewById(R.id.txtDateLabel)

        init {
            view.setOnClickListener {

                val position = bindingAdapterPosition

                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                val previous = selectedPosition

                selectedPosition = position

                if (previous != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previous)
                }

                notifyItemChanged(selectedPosition)

                onClick(dates[position])
            }
        }
    }

    inner class CustomViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

//        val txtCustomDateLabel: TextView =
//            view.findViewById(R.id.txtCustomDateLabel)

        init {
            view.setOnClickListener {

                val position = bindingAdapterPosition

                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                val today = Calendar.getInstance()

                DatePickerDialog(
                    view.context,
                    { _, year, month, dayOfMonth ->

                        val picked = Calendar.getInstance()

                        picked.set(
                            Calendar.YEAR,
                            year
                        )

                        picked.set(
                            Calendar.MONTH,
                            month
                        )

                        picked.set(
                            Calendar.DAY_OF_MONTH,
                            dayOfMonth
                        )

                        // Store actual API date
                        customDate =
                            apiDateFormat.format(picked.time)

                        val previous = selectedPosition

                        selectedPosition = position

                        notifyItemChanged(previous)
                        notifyItemChanged(selectedPosition)

                        // Return yyyy-MM-dd
                        onClick(customDate!!)

                    },
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH)
                ).apply {

                    // Cannot select previous dates
                    datePicker.minDate =
                        today.timeInMillis

                }.show()
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (position == customPosition) {
            TYPE_CUSTOM
        } else {
            TYPE_DATE
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater =
            LayoutInflater.from(parent.context)

        return if (viewType == TYPE_CUSTOM) {

            CustomViewHolder(
                inflater.inflate(
                    R.layout.item_date_chip_custom,
                    parent,
                    false
                )
            )

        } else {

            ViewHolder(
                inflater.inflate(
                    R.layout.item_date_chip,
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount(): Int =
        dates.size + 1

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        if (holder is ViewHolder) {

            val apiDate = dates[position]

            val parsedDate = try {
                apiDateFormat.parse(apiDate)
            } catch (e: Exception) {
                null
            }

            holder.txtDateLabel.text =
                if (parsedDate != null) {

                    SimpleDateFormat(
                        "EEE MMM dd",
                        Locale.ENGLISH
                    ).format(parsedDate)

                } else {
                    apiDate
                }

            holder.itemView.isSelected =
                position == selectedPosition
        }
    }

    fun getSelectedDate(): String? {

        return if (selectedPosition == customPosition) {
            customDate
        } else {
            dates.getOrNull(selectedPosition)
        }
    }
}