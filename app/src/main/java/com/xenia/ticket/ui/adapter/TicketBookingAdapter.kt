package com.xenia.ticket.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.data.listeners.OnBookingTicketClick
import com.xenia.ticket.data.listeners.OnTicketClickListener
import com.xenia.ticket.data.network.model.TicketDto

import com.xenia.ticket.data.room.entity.Ticket
import com.xenia.ticket.databinding.BookingTicketItemListRowBinding
import java.util.Locale

class TicketBookingAdapter(
    private val selectedLanguage: String,
    private val onBookingTicketClick: OnBookingTicketClick,
    private val onTicketClickListener: OnTicketClickListener
) : RecyclerView.Adapter<TicketBookingAdapter.TicketViewHolder>() {

    private var ticketItems: List<TicketDto> = emptyList()
    private var dbItemsMap: Map<Int, Ticket> = emptyMap()

    class TicketViewHolder(val binding: BookingTicketItemListRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = BookingTicketItemListRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TicketViewHolder(binding)
    }

    override fun getItemCount(): Int = ticketItems.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticketItem = ticketItems[position]
        val binding = holder.binding

        binding.txtDharshanName.text = when (selectedLanguage) {
            "ml" -> ticketItem.ticketNameMa ?: ticketItem.ticketName
            "ta" -> ticketItem.ticketNameTa ?: ticketItem.ticketName
            "te" -> ticketItem.ticketNameTe ?: ticketItem.ticketName
            "kn" -> ticketItem.ticketNameKa ?: ticketItem.ticketName
            "hi" -> ticketItem.ticketNameHi ?: ticketItem.ticketName
            "si" -> ticketItem.ticketNameSi ?: ticketItem.ticketName
            "mr" -> ticketItem.ticketNameMr ?: ticketItem.ticketName
            "pa" -> ticketItem.ticketNamePa ?: ticketItem.ticketName
            else -> ticketItem.ticketName
        }

        val dbItem = dbItemsMap[ticketItem.ticketId]
        val qty = dbItem?.daQty ?: 0

        binding.txtQty.text = qty.toString()

        val priceToShow =
            if (qty > 0) ticketItem.ticketAmount * qty
            else ticketItem.ticketAmount

        binding.txtDharshanPrice.text =
            "Rs. ${String.format(Locale.ENGLISH, "%.2f", priceToShow)}"

        binding.relPlus.setOnClickListener {
            onBookingTicketClick.onTicketPlusClick(ticketItem)
        }

        binding.relMinus.setOnClickListener {
            onBookingTicketClick.onTicketMinusClick(ticketItem)
        }

        binding.relNoneCardItem.setOnClickListener {
            onTicketClickListener.onTicketClick(ticketItem)
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateTickets(newTickets: List<TicketDto>) {
        this.ticketItems = newTickets

        notifyDataSetChanged()
    }



    @SuppressLint("NotifyDataSetChanged")
    fun updateDbItemsMap(newMap: Map<Int, Ticket>) {
        this.dbItemsMap = newMap
        notifyDataSetChanged()
    }
}
