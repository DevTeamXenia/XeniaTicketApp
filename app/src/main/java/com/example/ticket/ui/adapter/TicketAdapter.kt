package com.example.ticket.ui.adapter



import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ticket.R
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.repository.ActiveTicketRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.ActiveTicket
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketAdapter(
    private val context: Context,
    private val selectedLanguage: String,
    private val ticketRepository: TicketRepository,
    private val coroutineScope: CoroutineScope,
    private val onTicketClickListener: OnTicketClickListener
) : RecyclerView.Adapter<TicketAdapter.ViewHolder>() {

    private var ticketItems: List<ActiveTicket> = listOf()
    private var dbItemsMap: Map<Int, Ticket> = emptyMap()
    private var selectedItemPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_tickets_single_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ticketItem = ticketItems[position]
        holder.bind(ticketItem)
    }

    override fun getItemCount(): Int = ticketItems.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val relNoneCardItem: RelativeLayout = itemView.findViewById(R.id.relNoneCardItem)
        private val relCardItem: RelativeLayout = itemView.findViewById(R.id.relCardItem)
        private val txtTicketName1: TextView = itemView.findViewById(R.id.txt_darshan_name_1)
        private val txtTicketPrice1: TextView = itemView.findViewById(R.id.txt_darshan_price_1)
        private val txtTicketName2: TextView = itemView.findViewById(R.id.txt_darshan_name_2)
        private val txtTicketPrice2: TextView = itemView.findViewById(R.id.txt_darshan_price_2)
        private val imgClearCart: ImageView = itemView.findViewById(R.id.imgClearCart)
        private val txtTotalAmount: TextView = itemView.findViewById(R.id.totalAmount)

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(ticketItem: ActiveTicket) {
            itemView.post {
                coroutineScope.launch {
                    try {
                        // Replace cart logic if needed; using ticketId
                        val cartItem =
                            ticketRepository.getCartItemByTicketId(ticketItem.ticketId)

                        withContext(Dispatchers.Main) {
                            if (cartItem != null) {
                                relNoneCardItem.visibility = View.GONE
                                relCardItem.visibility = View.VISIBLE
                                txtTicketName2.text = when (selectedLanguage) {
                                    LANGUAGE_ENGLISH -> ticketItem.ticketName
                                    LANGUAGE_MALAYALAM -> ticketItem.ticketNameMa
                                    LANGUAGE_TAMIL -> ticketItem.ticketNameTa
                                    LANGUAGE_KANNADA -> ticketItem.ticketNameKa
                                    LANGUAGE_TELUGU -> ticketItem.ticketNameTe
                                    LANGUAGE_HINDI -> ticketItem.ticketNameHi
                                    LANGUAGE_SINHALA -> ticketItem.ticketNameSi
                                    LANGUAGE_MARATHI -> ticketItem.ticketNameMr
                                    LANGUAGE_PUNJABI -> ticketItem.ticketNamePa
                                    else -> ticketItem.ticketName
                                }
                                txtTicketPrice2.text = "Rs. ${ticketItem.ticketAmount}"
                                val formattedTotal = String.format("%.2f", cartItem.ticketTotalAmount)
                                txtTotalAmount.text = "Rs. $formattedTotal/-"
                            } else {
                                relNoneCardItem.visibility = View.VISIBLE
                                relCardItem.visibility = View.GONE
                                txtTicketName1.text = when (selectedLanguage) {
                                    LANGUAGE_ENGLISH -> ticketItem.ticketName
                                    LANGUAGE_MALAYALAM -> ticketItem.ticketNameMa
                                    LANGUAGE_TAMIL -> ticketItem.ticketNameTa
                                    LANGUAGE_KANNADA -> ticketItem.ticketNameKa
                                    LANGUAGE_TELUGU -> ticketItem.ticketNameTe
                                    LANGUAGE_HINDI -> ticketItem.ticketNameHi
                                    LANGUAGE_SINHALA -> ticketItem.ticketNameSi
                                    LANGUAGE_MARATHI -> ticketItem.ticketNameMr
                                    LANGUAGE_PUNJABI -> ticketItem.ticketNamePa
                                    else -> ticketItem.ticketName
                                }
                                val formattedRate = String.format("%.2f", ticketItem.ticketAmount)
                                txtTicketPrice1.text = "Rs. $formattedRate"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            itemView.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val previousSelectedItem = selectedItemPosition
                selectedItemPosition = currentPosition

                notifyItemChanged(previousSelectedItem)
                notifyItemChanged(selectedItemPosition)

                onTicketClickListener.onTicketClick(ticketItem)
            }

            imgClearCart.setOnClickListener {
                onTicketClickListener.onTicketClear(ticketItem)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTickets(newTickets: List<ActiveTicket>) {
        ticketItems = newTickets
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshCartState() {
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateDbItemsMap(newMap: Map<Int, Ticket>) {
        this.dbItemsMap = newMap
        notifyDataSetChanged()
    }
}
