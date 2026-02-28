package com.xenia.ticket.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import com.xenia.ticket.data.network.model.TransactionItem

class TransactionAdapter :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val items = mutableListOf<TransactionItem>()

    fun submitList(list: List<TransactionItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addMore(list: List<TransactionItem>) {
        val start = items.size
        items.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }
    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReceipt: TextView = itemView.findViewById(R.id.tvReceipt)
        private val tvTicket: TextView = itemView.findViewById(R.id.tvTicket)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvMode: TextView = itemView.findViewById(R.id.tvMode)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(item: TransactionItem) {
            tvReceipt.text = item.ReceiptNo.takeIf { !it.isNullOrEmpty() } ?: "--"
            tvTicket.text = item.TicketNo
            tvAmount.text = "₹ ${item.Amount}"
            tvMode.text = item.SourceType
            tvStatus.text = mapStatus(item.Status)

            tvStatus.setTextColor(
                when (item.Status) {
                    "S" -> Color.parseColor("#2E7D32")
                    "CANCELLED" -> Color.parseColor("#C62828")
                    "INITIATED" -> Color.parseColor("#EF6C00")
                    else -> Color.BLACK
                }
            )
        }

        private fun mapStatus(status: String): String = when (status) {
            "S" -> "SUCCESS"
            "CANCELLED" -> "CANCELLED"
            "INITIATED" -> "INITIATED"
            else -> status
        }
    }
}