package com.xenia.ticket.ui.adapter



import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import java.util.Locale
import kotlin.text.format


sealed class DisplayItem {
    data class Header(val title: String) : DisplayItem()
    data object ColumnHeader : DisplayItem()
    data class Item(
        val name: String,
        val qty: Double,
        val rate: Double,
        val total: Double
    ) : DisplayItem()

    data class TotalRow( val totalAmount: Double) : DisplayItem()
}


class ItemSummaryAdapter(private val items: List<DisplayItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_COLUMN_HEADER = 1
        private const val TYPE_ITEM = 2
        private const val TYPE_TOTAL = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DisplayItem.Header -> TYPE_HEADER
            is DisplayItem.ColumnHeader -> TYPE_COLUMN_HEADER
            is DisplayItem.Item -> TYPE_ITEM
            is DisplayItem.TotalRow -> TYPE_TOTAL
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false))
            TYPE_COLUMN_HEADER -> ColumnHeaderViewHolder(inflater.inflate(R.layout.item_column_header, parent, false))
            TYPE_ITEM -> ItemViewHolder(inflater.inflate(R.layout.item_row, parent, false))
            TYPE_TOTAL -> TotalViewHolder(inflater.inflate(R.layout.item_total, parent, false)) // <--- NEW
            else -> throw kotlin.IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DisplayItem.Header -> (holder as HeaderViewHolder).bind(item)
            is DisplayItem.ColumnHeader -> (holder as ColumnHeaderViewHolder).bind()
            is DisplayItem.Item -> (holder as ItemViewHolder).bind(item)
            is DisplayItem.TotalRow -> (holder as TotalViewHolder).bind(item)
        }
    }
    class TotalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label = view.findViewById<TextView>(R.id.tvTotalLabel)
        private val amount = view.findViewById<TextView>(R.id.tvTotalAmount)

        @SuppressLint("SetTextI18n")
        fun bind(item: DisplayItem.TotalRow) {
            amount.text = " %.2f".format(Locale.ENGLISH,item.totalAmount)
        }
    }
    override fun getItemCount() = items.size

    class ColumnHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val itemName = view.findViewById<TextView>(R.id.tvColItem)
        private val rate = view.findViewById<TextView>(R.id.tvColRate)
        private val qty = view.findViewById<TextView>(R.id.tvColQty)
        private val total = view.findViewById<TextView>(R.id.tvColTotal)
        @SuppressLint("SetTextI18n")
        fun bind() {
            itemName.text = "Ticket"
            rate.text = "Rate"
            qty.text = "Qty"
            total.text = "Total"
        }
    }
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvHeader)
        fun bind(item: DisplayItem.Header) {
            title.text = item.title
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.tvItemName)
        private val rate = view.findViewById<TextView>(R.id.tvRate)
        private val qty = view.findViewById<TextView>(R.id.tvQty)
        private val total = view.findViewById<TextView>(R.id.tvTotal)
        @SuppressLint("DefaultLocale")
        fun bind(item: DisplayItem.Item) {
            name.text = item.name
            rate.text = String.format(Locale.ENGLISH, "%.2f", item.rate)
            qty.text = item.qty.toString()
            total.text =  String.format(Locale.ENGLISH,"%.2f", item.total)
        }
    }
}