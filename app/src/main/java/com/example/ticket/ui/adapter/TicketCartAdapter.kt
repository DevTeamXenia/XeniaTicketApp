package com.example.ticket.ui.adapter

import com.example.ticket.R
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.example.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.example.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.example.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.example.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.example.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.example.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.example.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.example.ticket.utils.common.Constants.LANGUAGE_TELUGU
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class TicketCartAdapter(
    private val context: Context,
    private val selectedLanguage: String,
    private val from: String,
    private val onTicketCartClickListener: OnTicketCartClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var darshanItems: List<Ticket> = listOf()

    interface OnTicketCartClickListener {
        fun onDeleteClick(ticket: Ticket)
        fun onEditClick(ticket: Ticket)
    }

    companion object {
        private const val VIEW_TYPE_CART = 0
        private const val VIEW_TYPE_BOOKING = 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTickets(darshanItem: List<Ticket>) {
        darshanItems = darshanItem
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = darshanItems.size

    override fun getItemViewType(position: Int): Int {
        return if (from == "Booking") VIEW_TYPE_BOOKING else VIEW_TYPE_CART
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_BOOKING -> {
                val view = inflater.inflate(R.layout.ticket_booking_single_row, parent, false)
                BookingViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.ticket_cart_single_row, parent, false)
                CartViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val darshanItem = darshanItems[position]
        when (holder) {
            is CartViewHolder -> holder.bind(darshanItem)
            is BookingViewHolder -> holder.bind(darshanItem)
        }
    }

    // Cart ViewHolder
    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtQty: TextView = itemView.findViewById(R.id.txtQty)
        private val totalAmount: TextView = itemView.findViewById(R.id.totalAmount)
        private val imgDelete: ImageView = itemView.findViewById(R.id.imgDelete)
        private val txtEdit: RelativeLayout = itemView.findViewById(R.id.txtEdit)

        @SuppressLint("SetTextI18n")
        fun bind(ticketItem: Ticket) {
            txtName.text = when (selectedLanguage) {
                LANGUAGE_ENGLISH -> ticketItem.ticketName
                LANGUAGE_MALAYALAM -> ticketItem.ticketNameMa
                LANGUAGE_TAMIL -> ticketItem.ticketNameTa
                LANGUAGE_KANNADA -> ticketItem.ticketNameKa
                LANGUAGE_TELUGU -> ticketItem.ticketNameTe
                LANGUAGE_HINDI -> ticketItem.ticketNameHi
                LANGUAGE_SINHALA -> ticketItem.ticketNameSi
                LANGUAGE_PUNJABI -> ticketItem.ticketNamePa
                LANGUAGE_MARATHI -> ticketItem.ticketNameMr
                else -> ticketItem.ticketName
            }

            txtQty.text = "Rs. %.2f x %d".format(ticketItem.daRate, ticketItem.daQty)
            totalAmount.text = "Rs. %.2f/-".format(ticketItem.daTotalAmount)

            imgDelete.setOnClickListener {
                onTicketCartClickListener.onDeleteClick(ticketItem)
            }
            txtEdit.setOnClickListener {
                onTicketCartClickListener.onEditClick(ticketItem)
            }
        }
    }

    // Booking ViewHolder
    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtQty: TextView = itemView.findViewById(R.id.txtQty)
        private val txtRate: TextView = itemView.findViewById(R.id.txtRate)
        private val totalAmount: TextView = itemView.findViewById(R.id.totalAmount)

        @SuppressLint("SetTextI18n")
        fun bind(ticketItem: Ticket) {
            txtName.text = when (selectedLanguage) {
                LANGUAGE_ENGLISH -> ticketItem.ticketName
                LANGUAGE_MALAYALAM -> ticketItem.ticketNameMa
                LANGUAGE_TAMIL -> ticketItem.ticketNameTa
                LANGUAGE_KANNADA -> ticketItem.ticketNameKa
                LANGUAGE_TELUGU -> ticketItem.ticketNameTe
                LANGUAGE_HINDI -> ticketItem.ticketNameHi
                LANGUAGE_SINHALA -> ticketItem.ticketNameSi
                LANGUAGE_PUNJABI -> ticketItem.ticketNamePa
                LANGUAGE_MARATHI -> ticketItem.ticketNameMr
                else -> ticketItem.ticketName
            }

            txtQty.text = ticketItem.daQty.toString()
            txtRate.text = "Rs. ${ticketItem.daRate.toInt()}"
            totalAmount.text = "Rs. ${ticketItem.daTotalAmount.toInt()}/-"


        }
    }
}
