package com.example.ticket.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ticket.data.listeners.OnBookingTicketClick
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.network.model.TicketDto

import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.databinding.BookingTicketItemListRowBinding
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
        val ticketItems = ticketItems[position]
        val binding = holder.binding

        binding.txtDharshanName.text = when (selectedLanguage) {
            "ml" -> ticketItems.ticketNameMa ?: ticketItems.ticketName
            "ta" -> ticketItems.ticketNameTa ?: ticketItems.ticketName
            "te" -> ticketItems.ticketNameTe ?: ticketItems.ticketName
            "kn" -> ticketItems.ticketNameKa ?: ticketItems.ticketName
            "hi" -> ticketItems.ticketNameHi ?: ticketItems.ticketName
            "si" -> ticketItems.ticketNameSi ?: ticketItems.ticketName
            "mr" -> ticketItems.ticketNameMr ?: ticketItems.ticketName
            "pa" -> ticketItems.ticketNamePa ?: ticketItems.ticketName
            else -> ticketItems.ticketName
        }

        binding.txtDharshanPrice.text = "Rs. ${String.format(Locale.ENGLISH, "%.2f", ticketItems.ticketAmount)}"

        val dbItem = dbItemsMap[ticketItems.ticketId]
        val qty = dbItem?.daQty ?: 0
        val total = ticketItems.ticketAmount * qty

        binding.txtQty.text = qty.toString()
        if(qty != 0){
            binding.txtDharshanPrice.text = "Rs. ${String.format(Locale.ENGLISH, "%.2f", total)}"
        }

        binding.relPlus.setOnClickListener {
            onBookingTicketClick.onTicketPlusClick(ticketItems)
        }

        binding.relMinus.setOnClickListener {
            onBookingTicketClick.onTicketMinusClick(ticketItems)
        }

        binding.relNoneCardItem.setOnClickListener {
            onTicketClickListener.onTicketClick(ticketItems)
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
