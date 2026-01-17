package com.example.ticket.ui.sreens.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.network.model.TicketDto
import com.example.ticket.data.network.model.TicketPaymentRequest
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.PaymentRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.databinding.ActivityTicketCartBinding
import com.example.ticket.ui.adapter.TicketCartAdapter
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.example.ticket.ui.dialog.CustomTicketPopupDialogue
import com.example.ticket.utils.common.CommonMethod.generateNumericTransactionReferenceID
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.InactivityHandler
import com.example.ticket.utils.common.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import kotlin.getValue



class TicketCartActivity : AppCompatActivity(), TicketCartAdapter.OnTicketCartClickListener,
    OnTicketClickListener, CustomInactivityDialog.InactivityCallback,
    InactivityHandlerActivity, CustomInternetAvailabilityDialog.InternetAvailabilityListener{

    private lateinit var binding: ActivityTicketCartBinding
    private val ticketRepository: TicketRepository by inject()
    private lateinit var ticketCartAdapter: TicketCartAdapter
    private val sessionManager: SessionManager by inject()
    private val paymentRepository: PaymentRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private var formattedTotalAmount: String = ""

    private var idProofType : String? = ""
    private var devoteeName: String? = null
    private var devoteePhone: String? = null
    private var devoteeIdProof: String? = null
    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var inactivityDialog: CustomInactivityDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setLocale(this, sessionManager.getSelectedLanguage())
        devoteeName = intent.getStringExtra("name")
        devoteePhone = intent.getStringExtra("phno")
        idProofType = intent.getStringExtra("ID")
        devoteeIdProof = intent.getStringExtra("IDtype")
        idProofType = intent.getStringExtra("ID")
        inactivityDialog = CustomInactivityDialog(this)
        inactivityHandler =
            InactivityHandler(this, supportFragmentManager, inactivityDialog)
        binding.relDarshanCart.layoutManager = LinearLayoutManager(this)
        ticketCartAdapter =
            TicketCartAdapter(this, sessionManager.getSelectedLanguage(), "Dharshan", this)
        binding.relDarshanCart.adapter = ticketCartAdapter
        loadDarshanItems()
        binding.btnPay.setOnClickListener {
            postDarshanPaymentHistory("S", "Successful")
        }
    }
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun loadDarshanItems() {
        lifecycleScope.launch {
            val allDarshanTickets = ticketRepository.getAllTicketsInCart()
            val (totalAmount) = ticketRepository.getCartStatus()
            if (allDarshanTickets.isEmpty()) {
                startActivity(Intent(this@TicketCartActivity, TicketActivity::class.java))
                finish()
            } else {
                val firstItem = allDarshanTickets.first()
                binding.txtValName.text = firstItem.daName
                binding.txtValPhNo.text = firstItem.daPhoneNumber
                binding.txtValIdNo.text = firstItem.daProofId
                binding.txtIdNo.text = "$idProofType:"
                val bitmap = BitmapFactory.decodeByteArray(firstItem.daImg, 0, firstItem.daImg.size)
                binding.personImage.setImageBitmap(bitmap)
                binding.personImage.visibility = View.VISIBLE
                ticketCartAdapter.updateTickets(allDarshanTickets)
                formattedTotalAmount = String.format("%.2f", totalAmount)
                binding.btnPay.text = getString(R.string.pay) + "  Rs. " + formattedTotalAmount
                binding.btnPay.isEnabled = true
                binding.btnPay.setBackgroundColor(
                    ContextCompat.getColor(
                        this@TicketCartActivity,
                        R.color.primaryColor
                    )
                )

            }
        }
    }

    override fun onDeleteClick(ticket: Ticket) {
        TODO("Not yet implemented")
    }

    override fun onEditClick(ticketItem: Ticket) {
        val dialog = CustomTicketPopupDialogue()

        dialog.setData(
            ticketId = ticketItem.ticketId,
            ticketName = ticketItem.ticketName,
            ticketNameMa = ticketItem.ticketNameMa ?: "",
            ticketNameTa = ticketItem.ticketNameTa ?: "",
            ticketNameKa = ticketItem.ticketNameKa ?: "",
            ticketNameTe = ticketItem.ticketNameTe ?: "",
            ticketNameHi = ticketItem.ticketNameHi ?: "",
            ticketNameSi = ticketItem.ticketNameSi ?: "",
            ticketNamePa = ticketItem.ticketNamePa ?: "",
            ticketNameMr = ticketItem.ticketNameMr ?: "",
            ticketCtegoryId = ticketItem.ticketCategoryId,
            ticketCompanyId = ticketItem.ticketCompanyId,
            ticketRate = ticketItem.ticketAmount
        )

        dialog.setListener(this)

        dialog.show(supportFragmentManager, "CustomPopup")
    }
    private fun postDarshanPaymentHistory(
        status: String,
        statusDesc: String,
        retryCount: Int = 0
    ) {
        lifecycleScope.launch {

            val cartTickets = ticketRepository.getAllTicketsInCart()
            if (cartTickets.isEmpty()) return@launch

            val firstTicket = cartTickets.first()

            val imageBase64 = Base64.encodeToString(
                firstTicket.daImg ?: ByteArray(0),
                Base64.NO_WRAP
            )

            val itemsList = cartTickets.map { item ->
                TicketPaymentRequest.Item(
                    taCategoryId = item.ticketCategoryId,
                    TicketId = item.ticketId,
                    Quantity = item.daQty,
                    Rate = item.daRate
                )
            }

            val companyId = companyRepository.getCompany()?.companyId ?: 0

            val request = TicketPaymentRequest(
                CompanyId = companyId,
                UserId = sessionManager.getUserId(),
                Name = firstTicket.daName ?: "",
                tTranscationId = generateNumericTransactionReferenceID(),
                tCustRefNo = firstTicket.daCustRefNo ?: "",
                tNpciTransId = firstTicket.daNpciTransId ?: "",
                tIdProofNo = firstTicket.daProofId ?: "",
                tImage = imageBase64,
                PhoneNumber = firstTicket.daPhoneNumber ?: "",
                tPaymentStatus = status,
                tPaymentMode = "ONLINE",
                tPaymentDes = statusDesc,
                Items = itemsList
            )

            val token = sessionManager.getToken()
            if (token.isNullOrBlank()) {
                binding.btnPay.isEnabled = true
                showSnackbar(binding.root, "Authorization token missing")
                return@launch
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    paymentRepository.postTicket(
                        bearerToken = "Bearer $token",
                        request = request
                    )
                }

                Log.d("DARSHAN_REQUEST", Gson().toJson(request))
                Log.d("DARSHAN_RESPONSE", Gson().toJson(response))

                if (response.status.equals("OPEN", true)) {
                    val (totalAmount) = ticketRepository.getCartStatus()

                    handleDarshanTransactionStatus(
                        status,
                        response.orderId.toLong(),
                        totalAmount.toDouble()
                    )
                } else {
                    binding.btnPay.isEnabled = true
                    showSnackbar(binding.root, "Failed to post order")
                }

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    binding.btnPay.isEnabled = true
                    showSnackbar(binding.root, "Unauthorized: Please login again")
                } else if (retryCount < 3) {
                    postDarshanPaymentHistory(status, statusDesc, retryCount + 1)
                } else {
                    binding.btnPay.isEnabled = true
                    showSnackbar(binding.root, "Failed: ${e.message}")
                }
            } catch (e: Exception) {
                if (retryCount < 3) {
                    postDarshanPaymentHistory(status, statusDesc, retryCount + 1)
                } else {
                    binding.btnPay.isEnabled = true
                    showSnackbar(binding.root, "Failed: ${e.message}")
                }
            }
        }
    }



    private fun handleDarshanTransactionStatus(
        status: String,
        orderId: Long   ,
        totalAmount: Double,
    ) {
        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("from", "billing")
            putExtra("status", status)
            putExtra("amount", totalAmount.toString())
            putExtra("orderID", orderId)
            putExtra("transID", "BookingTest")
            putExtra("name",devoteeName)
            putExtra("phno", devoteePhone)
            putExtra("IDNO", devoteeIdProof)
            putExtra("ID", idProofType)

        }
        startActivity(intent)
        finish()
    }
    override fun onTicketClick(darshanItem: TicketDto) {
        TODO("Not yet implemented")
    }

    override fun onTicketClear(darshanItem: TicketDto) {
        TODO("Not yet implemented")
    }

    override fun onTicketAdded() {
        loadDarshanItems()
    }

    override fun resetInactivityTimer() {
        inactivityHandler.resetTimer()
    }


    override fun onResume() {
        super.onResume()
        inactivityHandler.resumeInactivityCheck()
    }

    override fun onPause() {
        super.onPause()
        inactivityHandler.pauseInactivityCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.cleanup()
    }

    override fun onRetryClicked() {
        inactivityHandler.resumeInactivityCheck()
        //generatePaymentToken(donationAmount!!)
    }
    override fun onDialogInactive() {
        inactivityHandler.resumeInactivityCheck()
        inactivityHandler.showDialogSafely()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        inactivityHandler.resetTimer()
        return super.dispatchTouchEvent(ev)
    }
}