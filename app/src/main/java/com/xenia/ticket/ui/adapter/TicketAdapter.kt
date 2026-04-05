package com.xenia.ticket.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import com.xenia.ticket.data.listeners.OnTicketClickListener
import com.xenia.ticket.data.network.model.ActiveItem
import com.xenia.ticket.data.repository.OrderRepository
import com.xenia.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.xenia.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.xenia.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.xenia.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.xenia.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.xenia.ticket.utils.common.Constants.LANGUAGE_TELUGU

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TicketAdapter(
    private val context: Context,
    private val selectedLanguage: String,
    private val ticketRepository: OrderRepository,
    private val coroutineScope: CoroutineScope,
    private val onTicketClickListener: OnTicketClickListener
) : RecyclerView.Adapter<TicketAdapter.ViewHolder>() {

    private var ticketItems: List<ActiveItem> = listOf()
    private var selectedItemPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_tickets_single_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = ticketItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ticketItems[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<ActiveItem>) {
        ticketItems = newItems
        notifyDataSetChanged()
    }

    fun updateSingleItem(id: Int) {
        val index = ticketItems.indexOfFirst { it.id == id }
        if (index != -1) notifyItemChanged(index)
    }

    private fun getNameByLanguage(item: ActiveItem): String {
        return when (selectedLanguage) {
            LANGUAGE_ENGLISH -> item.name
            LANGUAGE_MALAYALAM -> item.nameMa
            LANGUAGE_TAMIL -> item.nameTa
            LANGUAGE_KANNADA -> item.nameKa
            LANGUAGE_TELUGU -> item.nameTe
            LANGUAGE_HINDI -> item.nameHi
            LANGUAGE_SINHALA -> item.nameSi
            LANGUAGE_MARATHI -> item.nameMr
            LANGUAGE_PUNJABI -> item.namePa
            else -> item.name
        }!!
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val relNoneCardItem: RelativeLayout = itemView.findViewById(R.id.relNoneCardItem)
        private val relCardItem: RelativeLayout = itemView.findViewById(R.id.relCardItem)

        private val txtTicketName1: TextView = itemView.findViewById(R.id.txt_ticket_name_1)
        private val txtTicketPrice1: TextView = itemView.findViewById(R.id.txt_ticket_price_1)
        private val txtTicketCombo: TextView = itemView.findViewById(R.id.txt_ticket_combo)

        private val txtTicketName2: TextView = itemView.findViewById(R.id.txt_ticket_name_2)
        private val txtTicketPrice2: TextView = itemView.findViewById(R.id.txt_ticket_price_2)
        private val quantity: TextView = itemView.findViewById(R.id.txtQty)
        private val txtTotalAmount: TextView = itemView.findViewById(R.id.txt_ticket_total_price_2)

        private val imgClearCart: ImageView = itemView.findViewById(R.id.imgClearCart)

        @SuppressLint("SetTextI18n")
        fun bind(item: ActiveItem) {

            val name = getNameByLanguage(item)

            coroutineScope.launch {
                try {
                    val cartItem = ticketRepository.getCartItemByTicketId(item.id)

                    withContext(Dispatchers.Main) {
                        val isSelected = cartItem != null
                        if (isSelected) {
                            relNoneCardItem.visibility = View.GONE
                            relCardItem.visibility = View.VISIBLE

                            txtTicketName2.text = name

                            val qty = cartItem.daQty
                            val rate = item.amount

                            txtTicketPrice2.text =
                                "Rs. ${String.format(Locale.ENGLISH, "%.2f", rate)}"

                            quantity.text =
                                context.getString(R.string.txt_amount) + " " +
                                        String.format(Locale.ENGLISH, "%.2f", rate) +
                                        " * " +
                                        String.format(Locale.ENGLISH, "%d", qty)

                            val total = rate * qty
                            txtTotalAmount.text =
                                String.format(Locale.ENGLISH, "%.2f/-", total)

                        } else {
                            relNoneCardItem.visibility = View.VISIBLE
                            relCardItem.visibility = View.GONE

                            txtTicketName1.text = name

                            txtTicketPrice1.text =
                                "Rs. ${String.format(Locale.ENGLISH, "%.2f", item.amount)}"

                            if(item.combo){
                                txtTicketCombo.visibility = View.VISIBLE
                                txtTicketCombo.text = "Combo"
                            }else if(item.type == "SHOW"){
                                txtTicketCombo.visibility = View.VISIBLE
                                txtTicketCombo.text = "Show"
                            }else{
                                txtTicketCombo.visibility = View.GONE
                            }


                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val prev = selectedItemPosition
                selectedItemPosition = position

                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(position)

                onTicketClickListener.onTicketClick(item)
            }

            imgClearCart.setOnClickListener {
                onTicketClickListener.onTicketClear(item)
            }
        }
    }
}