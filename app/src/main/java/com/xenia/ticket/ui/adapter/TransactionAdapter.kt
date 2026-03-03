package com.xenia.ticket.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import com.xenia.ticket.data.network.model.TransactionDetailItem

class TransactionAdapter(
    private val onDetailClick: (TransactionDetailItem) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val items = mutableListOf<TransactionDetailItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<TransactionDetailItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view, onDetailClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class TransactionViewHolder(
        itemView: View,
        private val onDetailClick: (TransactionDetailItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvReceipt: TextView = itemView.findViewById(R.id.tvReceipt)
        private val tvTicket: TextView = itemView.findViewById(R.id.tvTicket)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvMode: TextView = itemView.findViewById(R.id.tvMode)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val imgDetail: ImageView = itemView.findViewById(R.id.imgDetail)

        @SuppressLint("SetTextI18n")
        fun bind(item: TransactionDetailItem) {
            tvReceipt.text = item.ReceiptNo.ifEmpty { "--" }
            tvTicket.text = item.TicketNo
            tvAmount.text = "₹ ${item.TotalAmount}"
            tvMode.text = item.PaymentMode
            tvStatus.text = mapStatus(item.PaymentStatus)

            imgDetail.setOnClickListener {
                onDetailClick(item)
            }
        }

        private fun mapStatus(status: String): String = when (status) {
            "S" -> "SUCCESS"
            "CANCELLED" -> "CANCELLED"
            "INITIATED" -> "INITIATED"
            else -> status
        }
    }
}

