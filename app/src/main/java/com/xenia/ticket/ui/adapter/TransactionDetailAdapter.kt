package com.xenia.ticket.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import com.xenia.ticket.data.network.model.TransactionItem

class TransactionDetailAdapter :
    RecyclerView.Adapter<TransactionDetailAdapter.TransactionViewHolder>() {

    private val items = mutableListOf<TransactionItem>()

    fun submitList(list: List<TransactionItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_detail, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvTicketId: TextView = itemView.findViewById(R.id.tvTicketId)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val tvRate: TextView = itemView.findViewById(R.id.tvRate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        @SuppressLint("SetTextI18n")
        fun bind(item: TransactionItem) {
            tvTicketId.text = "Ticket Name : ${item.TicketName}"
            tvQty.text = "Qty : ${item.Quantity}"
            tvRate.text = "Rate : ₹${item.Rate}"
            tvAmount.text = "Amount : ₹${item.Amount}"
            tvStatus.text = item.Status
        }
    }
}