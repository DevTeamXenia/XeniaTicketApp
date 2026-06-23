package com.xenia.ticket.ui.screens.kiosk

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.xenia.ticket.R
import com.xenia.ticket.data.repository.CompanySettingsRepository
import com.xenia.ticket.data.repository.OrderRepository
import com.xenia.ticket.data.room.entity.Orders
import com.xenia.ticket.databinding.ActivityPaymentBinding
import com.xenia.ticket.ui.screens.billing.BillingTicketActivity
import com.xenia.ticket.utils.common.CommonMethod.setLocale
import com.xenia.ticket.utils.common.SessionManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.posprinter.IDeviceConnection
import net.posprinter.IPOSListener
import net.posprinter.POSConnect
import net.posprinter.POSConst
import net.posprinter.POSPrinter
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.getValue
import androidx.core.graphics.set
import org.json.JSONArray
import org.json.JSONObject
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xenia.ticket.data.network.model.SeatAllocationDto
import com.xenia.ticket.data.repository.TicketRepository
import java.io.ByteArrayOutputStream
import java.io.File
import com.xenia.ticket.utils.pineLab.PlutusConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlin.time.Duration.Companion.milliseconds


class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private val sessionManager: SessionManager by inject()
    private val ticketRepository: OrderRepository by inject()
    private val companyRepository: CompanySettingsRepository by inject()
    private var curConnect: IDeviceConnection? = null
    private var prefix: String? = null
    private var status: String? = null
    private var amount: String? = null
    private var transID: String? = null
    private var name: String? = null
    private var ticket: String? = null
    private var orderID: String? = null
    private var id: String? = null
    private var idProof: String? = null
    private var idProofMode: String? = null
    private var phoneNo: String? = null
    private var from: String? = null
    private var selectedLanguage: String? = null
    private var isBound = false
    private var serverMessenger: Messenger? = null
    private var seatAllocations: List<SeatAllocationDto> = emptyList()

    private val activeTicketRepository: TicketRepository by inject()

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        from = intent.getStringExtra("from")
        prefix = intent.getStringExtra("prefix")
        ticket = intent.getStringExtra("ticket")
        status = intent.getStringExtra("status")
        amount = intent.getStringExtra("amount")
        transID = intent.getStringExtra("transID")
        orderID = intent.getStringExtra("orderID")
        phoneNo = intent.getStringExtra("phno")
        idProof = intent.getStringExtra("IDNO")
        idProofMode = intent.getStringExtra("ID")
        name = intent.getStringExtra("name")
        id = intent.getStringExtra("id")
        val seatJson = intent.getStringExtra("seatAllocations")

        seatAllocations = if (!seatJson.isNullOrEmpty()) {
            Gson().fromJson(
                seatJson,
                object : TypeToken<List<SeatAllocationDto>>() {}.type
            )
        } else {
            emptyList()
        }

        selectedLanguage = if (from == "billing")
            sessionManager.getBillingSelectedLanguage()
        else
            sessionManager.getSelectedLanguage()

        setLocale(this@PaymentActivity, selectedLanguage)

        if (status.equals("S")) {

            binding.lottieSuccess.visibility = View.VISIBLE
            binding.lottieSuccess.playAnimation()

            binding.linSuccess.visibility = View.VISIBLE
            binding.linFailed.visibility = View.GONE
            val amountDouble = amount?.toDoubleOrNull() ?: 0.00
            val formattedAmount = String.format(Locale.ENGLISH, "%.2f", amountDouble)
            binding.txtAmount.text = getString(R.string.amount) + " " + formattedAmount
            if (transID.isNullOrEmpty()) {
                binding.txtTransId.visibility = View.GONE
            } else {
                binding.txtTransId.text = getString(R.string.transcation_id) + " " + transID
            }
            if (!name.isNullOrEmpty()) {
                binding.txtName.visibility = View.VISIBLE
                binding.txtName.text = getString(R.string.pay_name) + " " + name
            }

            sendTicketViaWhatsApp()

            if (sessionManager.getSelectedPrinter() == "KIOSK") {
                configPrinter()
            } else {
                bindPineLabsService()
            }

        } else {
            binding.linSuccess.visibility = View.GONE
            binding.linFailed.visibility = View.VISIBLE
            binding.lottiefail.visibility = View.VISIBLE
            binding.lottiefail.playAnimation()

            redirect()
        }

    }

    private fun sendTicketViaWhatsApp() {
        val bearerToken = sessionManager.getToken()
        if (bearerToken.isNullOrEmpty()) {
            Log.e("PaymentActivity", "Bearer token is null or empty")
            return
        }

        if (id.isNullOrEmpty()) {
            Log.e("PaymentActivity", "Order ID is null or empty")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val orderIdInt = id?.toIntOrNull()
                if (orderIdInt == null) {
                    Log.e("PaymentActivity", "Invalid Order ID format: $orderID")
                    return@launch
                }

                val result = companyRepository.sendWhatsApp(bearerToken, orderIdInt)

                result.fold(
                    onSuccess = { message ->
                        Log.d("PaymentActivity", "WhatsApp response: $message")
                    },
                    onFailure = { error ->
                        Log.e("PaymentActivity", "Error sending WhatsApp ticket: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("PaymentActivity", "Unexpected error: ${e.message}")
            }
        }
    }

    private fun bindPineLabsService() {
        val intent = Intent().apply {
            action = PlutusConstants.PLUTUS_ACTION
            setPackage(PlutusConstants.PLUTUS_PACKAGE)
        }
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serverMessenger = Messenger(service)
            isBound = true
            Log.d("PLUTUS", "Service connected")
            printReceiptPlutus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverMessenger = null
            isBound = false
            Log.d("PLUTUS", "Service disconnected")
        }
    }

    private fun configPrinter() {
        val selectedPrinter = sessionManager.getSelectedPrinter()
        when (selectedPrinter) {
            "FALCON", "KIOSK" -> {
                try {
                    POSConnect.init(applicationContext)
                    val entries = POSConnect.getUsbDevices(applicationContext)

                    if (entries.isNotEmpty()) {
                        try {
                            printReceipt(entries[0])
                        } catch (e: Exception) {
                            Toast.makeText(
                                this,
                                "Error printing receipt: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        redirect()

                    }
                } catch (_: Exception) {
                    redirect()
                }
            }

            "PineLabs" -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.txtPrinting.visibility = View.VISIBLE
                ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.Main) {
                    printReceiptPlutus()
                }
            }

            else -> {
                val printIntent = Intent(this, PrinterSettingActivity::class.java).apply {
                    putExtra("from", from)
                    putExtra("status", status)
                    putExtra("amount", amount)
                    putExtra("transID", transID)
                    putExtra("orderID", orderID)
                    putExtra("phno", phoneNo)
                    putExtra("ps", "pd")
                }
                startActivity(printIntent)
                finish()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val connectListener = IPOSListener { code, _ ->
        val selectedPrinter = sessionManager.getSelectedPrinter()
        when (code) {
            POSConnect.CONNECT_SUCCESS -> {
                if (selectedPrinter == "FALCON" || selectedPrinter == "KIOSK" || selectedPrinter == null) {
                    GlobalScope.launch {
                        delay(500.milliseconds)
                        initReceiptPrint()
                    }
                }
            }

            POSConnect.CONNECT_FAIL,
            POSConnect.CONNECT_INTERRUPT,
            POSConnect.SEND_FAIL,
            POSConnect.USB_DETACHED,
            POSConnect.USB_ATTACHED -> {
                redirect()
            }
        }
    }

    private fun printReceipt(pathName: String) {
        connectUSB(pathName)
    }

    private fun connectUSB(pathName: String) {
        curConnect?.close()
        curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB)
        curConnect!!.connect(pathName, connectListener)
    }

    @SuppressLint("DefaultLocale")
    private fun initReceiptPrint() {
        lifecycleScope.launch {

            val ticketItems = ticketRepository.getAllTicketsInCart()
            val currentDate =
                SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH)
                    .format(Date())

            val headerBitmap = withContext(Dispatchers.IO) {
                loadBitmapFromCache(cacheDir, "company_header.png")
            }

            val footerBitmap = withContext(Dispatchers.IO) {
                loadBitmapFromCache(cacheDir, "company_footer.png")
            }

            val receiptBitmap: Bitmap =
                if (companyRepository.getDefaultLanguage() == selectedLanguage) {
                    generateReceiptBitmapDefault(
                        currentDate,
                        transID,
                        orderID.toString(),
                        ticketItems,
                        selectedLanguage!!,
                        seatAllocations = seatAllocations
                    )
                } else {
                    generateReceiptBitmap(
                        currentDate,
                        transID,
                        orderID.toString(),
                        ticketItems,
                        selectedLanguage!!,
                        seatAllocations = seatAllocations
                    )
                }

            val printer = POSPrinter(curConnect)

            try {
                headerBitmap?.scale(550, 200)?.let { scaled ->
                    printer.printBitmap(
                        scaled,
                        POSConst.ALIGNMENT_CENTER,
                        500
                    )
                    printer.feedLine(2)
                    delay(300.milliseconds)
                    scaled.recycle()
                }

                printLargeBitmap(printer, receiptBitmap)
                printer.feedLine(2)
                delay(300.milliseconds)

                footerBitmap?.scale(550, 100)?.let { scaled ->
                    printer.printBitmap(
                        scaled,
                        POSConst.ALIGNMENT_CENTER,
                        500
                    )
                    printer.feedLine(3)
                    delay(400.milliseconds)
                    printer.cutHalfAndFeed(1)
                    scaled.recycle()
                } ?: printer.cutHalfAndFeed(1)

            } catch (e: Exception) {
                Log.e("ReceiptPrint", "Printing error: ${e.message}")
                printer.cutHalfAndFeed(1)
            }

            receiptBitmap.recycle()
            headerBitmap?.recycle()
            footerBitmap?.recycle()

            delay(2000.milliseconds)

            redirect()
        }
    }

    @SuppressLint("DefaultLocale")
    private suspend fun generateReceiptBitmap(
        currentDate: String,
        transID: String?,
        orderID: String?,
        ticket: List<Orders>,
        selectedLanguage: String,
        seatAllocations: List<SeatAllocationDto>,
    ): Bitmap {

        val width = 576
        val paint = Paint().apply {
            isAntiAlias = true
        }

        val indicTypeface = try {
            Typeface.createFromAsset(this.assets, "fonts/noto_sans_kannada_regular.ttf")
        } catch (_: Exception) {
            Typeface.DEFAULT
        }

        paint.typeface = indicTypeface
        val defaultLang = companyRepository.getDefaultLanguage().toString()

        val labelReceiptNo = getLocalizedString("Receipt No", selectedLanguage)
        val labelDate = getLocalizedString("Date", selectedLanguage)
        val labelItem = getLocalizedString("Ticket", selectedLanguage)
        val labelPrice = getLocalizedString("Price", selectedLanguage)
        val labelAmount = getLocalizedString("Amount", selectedLanguage)
        val labelUPI = getLocalizedString("UPI Reference No", selectedLanguage)
        val labelQty = getLocalizedString("Qty", selectedLanguage)
        val labelPhoneNumber = getLocalizedString("Phone No", selectedLanguage)
        val labelTotalAmount = getLocalizedString("Total Amount", selectedLanguage)
        val labelName = getLocalizedString("Name", selectedLanguage)
        val labelAdult = getLocalizedString("Adult", selectedLanguage)
        val labelChild = getLocalizedString("Child", selectedLanguage)

        val labelDReceiptNo = getLocalizedString("Receipt No", defaultLang)
        val labelDDate = getLocalizedString("Date", defaultLang)
        val labelDPhonenumber = getLocalizedString("Phone No", defaultLang)
        val labelDName = getLocalizedString("Name", defaultLang)
        val labelDItem = getLocalizedString("Ticket", defaultLang)
        val labelDPrice = getLocalizedString("Price", defaultLang)
        val labelDAmount = getLocalizedString("Amount", defaultLang)
        val labelDQty = getLocalizedString("Qty", defaultLang)
        val labelDAdult = getLocalizedString("Adult", defaultLang)
        val labelDChild = getLocalizedString("Child", defaultLang)

        val printDefaultLang =
            !(selectedLanguage.lowercase() == "en" && defaultLang.lowercase() == "en")

        val tempBitmap = createBitmap(width, 10000)
        val tempCanvas = Canvas(tempBitmap)

        paint.textSize = 30f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER

        val receiptTitle = when (selectedLanguage) {
            "ml" -> "പ്രവേശന ടിക്കറ്റ്"
            "kn" -> "ಪ್ರವೇಶ ಟಿಕೆಟ್"
            "ta" -> "நுழைவு டிக்கெட்"
            "te" -> "ప్రవేశ టికెట్"
            "hi" -> "प्रवेश टिकट"
            "pa" -> "ਪ੍ਰਵੇਸ਼ ਟਿਕਟ"
            "mr" -> "प्रवेश तिकीट"
            "si" -> "ප්‍රවේශ ටිකට්"
            else -> "Entry Ticket"
        }

        val receiptDTitle = when (defaultLang) {
            "ml" -> "പ്രവേശന ടിക്കറ്റ്"
            "kn" -> "ಪ್ರವೇಶ ಟಿಕೆಟ್"
            "ta" -> "நுழைவு டிக்கெட்"
            "te" -> "ప్రవేశ టికెట్"
            "hi" -> "प्रवेश टिकट"
            "pa" -> "ਪ੍ਰਵੇਸ਼ ਟਿਕਟ"
            "mr" -> "प्रवेश तिकीट"
            "si" -> "ප්‍රවේශ ටිකට්"
            else -> "Entry Ticket"
        }

        tempCanvas.drawText(receiptTitle, width / 2f, 40f, paint)
        if (printDefaultLang) {
            tempCanvas.drawText(receiptDTitle, width / 2f, 80f, paint)
        }

        var yOffset = 130f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 22f

        tempCanvas.drawText("$labelReceiptNo($labelDReceiptNo): ${prefix}$orderID", 20f, yOffset, paint)
        yOffset += 35f
        tempCanvas.drawText("$labelDate($labelDDate): $currentDate", 20f, yOffset, paint)
        yOffset += 35f
        tempCanvas.drawText("$labelName($labelDName): $name", 20f, yOffset, paint)
        yOffset += 35f
        tempCanvas.drawText("$labelPhoneNumber($labelDPhonenumber): $phoneNo", 20f, yOffset, paint)
        yOffset += 35f

        tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
        yOffset += 30f
        paint.textAlign = Paint.Align.LEFT
        tempCanvas.drawText(labelItem, 20f, yOffset, paint)
        paint.textAlign = Paint.Align.CENTER
        tempCanvas.drawText(labelPrice, width * 0.5f, yOffset, paint)
        tempCanvas.drawText(labelQty, width * 0.65f, yOffset, paint)
        paint.textAlign = Paint.Align.RIGHT
        tempCanvas.drawText(labelAmount, width - 30f, yOffset, paint)
        yOffset += 30f

        if (printDefaultLang) {
            paint.textAlign = Paint.Align.LEFT
            tempCanvas.drawText(labelDItem, 20f, yOffset, paint)
            paint.textAlign = Paint.Align.CENTER
            tempCanvas.drawText(labelDPrice, width * 0.5f, yOffset, paint)
            tempCanvas.drawText(labelDQty, width * 0.65f, yOffset, paint)
            paint.textAlign = Paint.Align.RIGHT
            tempCanvas.drawText(labelDAmount, width - 30f, yOffset, paint)
            yOffset += 30f
        }

        paint.strokeWidth = 2f
        tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
        yOffset += 40f

        var totalAmount = 0.0
        val seatMapQueue = mutableMapOf<Int, MutableList<String>>()

        seatAllocations.forEach {
            val existing = seatMapQueue[it.scheduleId] ?: mutableListOf()
            existing.addAll(it.seats)
            seatMapQueue[it.scheduleId] = existing
        }

        for (item in ticket) {

            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.ticketRate)
            var qtyStr= "";
            qtyStr = if(item.ticketChild)
                item.ticketChildQty.toString()
            else
                item.ticketQty.toString()

            val qtyChildStr = item.ticketChildQty.toString()
            val priceChildStr = String.format("%.2f", item.ticketChildRate)

            val amountStr = String.format(Locale.ENGLISH, "%.2f", item.ticketTotalAmount)
            totalAmount += item.ticketTotalAmount

            paint.textAlign = Paint.Align.LEFT

            val itemName = when (selectedLanguage.lowercase()) {
                "ml" -> item.ticketNameMa
                "hi" -> item.ticketNameHi
                "ta" -> item.ticketNameTa
                "kn" -> item.ticketNameKa
                "te" -> item.ticketNameTe
                "si" -> item.ticketNameSi!!
                "pa" -> item.ticketNamePa
                "mr" -> item.ticketNameMr
                else -> item.ticketName
            }

            val maxWidth = width * 0.95f

            yOffset = drawMultilineText(
                tempCanvas,
                itemName ?: "",
                20f,
                yOffset,
                maxWidth,
                paint
            ) + 10f

            paint.textSize = 22f

            if (item.ticketCombo) {

                val comboResult = activeTicketRepository.getComboResult(item.ticketId)

                val showName = comboResult.names.lastOrNull()
                val subTickets = comboResult.names.dropLast(1)

                subTickets.forEach {

                    val cleanText = it
                        .replace("\\s+".toRegex(), " ")
                        .trim()

                    if (cleanText.isNotEmpty()) {

                        val newY = drawMultilineText(
                            tempCanvas,
                            cleanText,
                            40f,
                            yOffset,
                            width * 0.9f,
                            paint
                        )

                        yOffset = newY + 2f
                    }
                }

                showName?.let {
                    yOffset = drawMultilineText(
                        tempCanvas, it, 40f, yOffset, width * 0.9f, paint
                    )
                    yOffset += 10f
                    if (item.scheduleId != 0) {

                        tempCanvas.drawText(
                            item.screenName + " - " + item.scheduleTime,
                            60f,
                            yOffset,
                            paint
                        )
                        yOffset += 35f

                        val adultQty = item.ticketQty
                        val childQty = item.ticketChildQty
                        val totalQty = adultQty + childQty

                        val allSeats = seatMapQueue[item.scheduleId] ?: emptyList()

                        var adultSeats = if (adultQty > 0) {
                            allSeats.take(adultQty)
                        } else emptyList()

                        var childSeats = if (childQty > 0) {
                            allSeats.drop(adultQty).take(childQty)
                        } else emptyList()


                        if (adultSeats.size < adultQty && adultSeats.isNotEmpty()) {
                            val duplicatedSeats = mutableListOf<String>()
                            while (duplicatedSeats.size < adultQty) {
                                duplicatedSeats.addAll(adultSeats)
                            }
                            adultSeats = duplicatedSeats.take(adultQty)
                        }

                        if (childSeats.size < childQty && childSeats.isNotEmpty()) {
                            val duplicatedSeats = mutableListOf<String>()
                            while (duplicatedSeats.size < childQty) {
                                duplicatedSeats.addAll(childSeats)
                            }
                            childSeats = duplicatedSeats.take(childQty)
                        }

                        paint.textSize = 28f

                        if (adultSeats.isNotEmpty()) {
                            val adultSeatText = if (printDefaultLang) {
                                "$labelAdult Seats: ${adultSeats.joinToString(", ")}"
                            } else {
                                "$labelAdult Seats: ${adultSeats.joinToString(", ")}"
                            }
                            yOffset = drawMultilineText(
                                tempCanvas, adultSeatText, 20f, yOffset, width * 0.9f, paint
                            ) + 10f
                        }

                        if (childSeats.isNotEmpty()) {
                            val childSeatText = if (printDefaultLang) {
                                "$labelChild Seats: ${childSeats.joinToString(", ")}"
                            } else {
                                "$labelChild Seats: ${childSeats.joinToString(", ")}"
                            }
                            yOffset = drawMultilineText(
                                tempCanvas, childSeatText, 20f, yOffset, width * 0.9f, paint
                            ) + 10f
                        }

                        val usedSeats = (adultSeats + childSeats).toSet().toList()
                        seatMapQueue[item.scheduleId]?.removeAll(usedSeats)
                    }
                }

            } else if (item.ticketType == "SHOW") {

                tempCanvas.drawText(
                    item.screenName + " - " + item.scheduleTime,
                    20f,
                    yOffset,
                    paint
                )
                yOffset += 35f

                val adultQty = item.ticketQty
                val childQty = item.ticketChildQty
                val totalQty = adultQty + childQty

                val allSeats = seatMapQueue[item.scheduleId] ?: emptyList()

                var adultSeats = if (adultQty > 0) {
                    allSeats.take(adultQty)
                } else emptyList()

                var childSeats = if (childQty > 0) {
                    allSeats.drop(adultQty).take(childQty)
                } else emptyList()


                if (adultSeats.size < adultQty && adultSeats.isNotEmpty()) {
                    val duplicatedSeats = mutableListOf<String>()
                    while (duplicatedSeats.size < adultQty) {
                        duplicatedSeats.addAll(adultSeats)
                    }
                    adultSeats = duplicatedSeats.take(adultQty)
                }

                if (childSeats.size < childQty && childSeats.isNotEmpty()) {
                    val duplicatedSeats = mutableListOf<String>()
                    while (duplicatedSeats.size < childQty) {
                        duplicatedSeats.addAll(childSeats)
                    }
                    childSeats = duplicatedSeats.take(childQty)
                }

                if (adultSeats.isNotEmpty() || childSeats.isNotEmpty()) {
                    paint.textSize = 28f

                    if (adultSeats.isNotEmpty()) {
                        val adultSeatText = if (printDefaultLang) {
                            "$labelAdult($labelDAdult) Seats: ${adultSeats.joinToString(", ")}"
                        } else {
                            "$labelAdult Seats: ${adultSeats.joinToString(", ")}"
                        }
                        yOffset = drawMultilineText(
                            tempCanvas, adultSeatText, 20f, yOffset, width * 0.9f, paint
                        ) + 10f
                    }

                    if (childSeats.isNotEmpty()) {
                        val childSeatText = if (printDefaultLang) {
                            "$labelChild($labelDChild) Seats: ${childSeats.joinToString(", ")}"
                        } else {
                            "$labelChild Seats: ${childSeats.joinToString(", ")}"
                        }
                        yOffset = drawMultilineText(
                            tempCanvas, childSeatText, 20f, yOffset, width * 0.9f, paint
                        ) + 10f
                    }

                    // Remove used seats
                    val usedSeats = (adultSeats + childSeats).toSet().toList()
                    seatMapQueue[item.scheduleId]?.removeAll(usedSeats)
                }
            }

            yOffset += 35f
            paint.textSize = 22f
            paint.textAlign = Paint.Align.CENTER

            if (item.ticketQty > 0) {
                tempCanvas.drawText(priceStr, width * 0.5f, yOffset, paint)
                tempCanvas.drawText(item.ticketQty.toString(), width * 0.65f, yOffset, paint)
                yOffset += 35f
            }

            if (qtyChildStr != "0" && priceChildStr != "0" && item.ticketChildQty > 0) {
                paint.textAlign = Paint.Align.CENTER
                tempCanvas.drawText(priceChildStr, width * 0.5f, yOffset, paint)
                tempCanvas.drawText(qtyChildStr, width * 0.65f, yOffset, paint)
                yOffset += 35f
            }

            paint.textAlign = Paint.Align.RIGHT
            tempCanvas.drawText(amountStr, width - 40f, yOffset, paint)

            yOffset += 45f
        }

        paint.strokeWidth = 2f
        tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
        yOffset += 60f

        paint.textAlign = Paint.Align.RIGHT
        tempCanvas.drawText(
            "$labelTotalAmount: ${String.format("%.2f", totalAmount)}",
            width - 20f,
            yOffset,
            paint
        )
        yOffset += 25f

        if (!transID.isNullOrEmpty()) {
            paint.textSize = 18f
            paint.textAlign = Paint.Align.RIGHT
            tempCanvas.drawText(
                "$labelUPI: $transID",
                width - 20f,
                yOffset,
                paint
            )
            yOffset += 35f
        }

        if (companyRepository.getPaymentQr() == "True") {
            yOffset += 45f
            paint.strokeWidth = 2f
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
            yOffset += 30f

            generateQRCode()?.let { qrBitmap ->
                val qrSize = 300
                val qrX = (width - qrSize) / 2f

                val bgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                tempCanvas.drawRect(qrX, yOffset, qrX + qrSize, yOffset + qrSize, bgPaint)
                tempCanvas.drawBitmap(qrBitmap, qrX, yOffset, paint)

                val borderPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                tempCanvas.drawRect(qrX, yOffset, qrX + qrSize, yOffset + qrSize, borderPaint)

                yOffset += qrSize + 30f

                paint.textSize = 18f
                paint.textAlign = Paint.Align.CENTER
                val scanText = getLocalizedString("Scan QR Code", selectedLanguage)
                tempCanvas.drawText(scanText, width / 2f, yOffset, paint)
                yOffset += 25f

                tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
                yOffset += 25f
            }
        }

        if (companyRepository.getTermAndPolicy() != null) {
            val termsAndConditions = companyRepository.getTermAndPolicy()
            if (!termsAndConditions.isNullOrEmpty()) {
                yOffset += 30f

                paint.strokeWidth = 2f
                paint.textSize = 18f
                paint.textAlign = Paint.Align.CENTER
                tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
                yOffset += 25f

                paint.textSize = 20f
                tempCanvas.drawText("TERMS & CONDITIONS", width / 2f, yOffset, paint)
                yOffset += 20f

                tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
                yOffset += 25f

                paint.textSize = 20f
                paint.textAlign = Paint.Align.LEFT

                val maxCharsPerLine = 55
                val lines = formatTermsTextForBitmap(termsAndConditions, maxCharsPerLine)

                for (line in lines) {
                    if (line.isNotEmpty()) {
                        tempCanvas.drawText(line, 20f, yOffset, paint)
                    }
                    yOffset += 20f
                }
            }
        }

        val extraPadding = if (companyRepository.getPaymentQr() == "True") 200 else 50
        val finalBitmap = createBitmap(width, (yOffset + extraPadding).toInt())
        Canvas(finalBitmap).drawBitmap(tempBitmap, 0f, 0f, null)
        tempBitmap.recycle()

        return finalBitmap
    }

    @SuppressLint("DefaultLocale")
    private suspend fun generateReceiptBitmapDefault(
        currentDate: String,
        transID: String?,
        orderID: String?,
        ticket: List<Orders>,
        selectedLanguage: String,
        seatAllocations: List<SeatAllocationDto>,
    ): Bitmap {
        val width = 576
        val paint = Paint().apply { isAntiAlias = true }

        val labelReceiptNo = getLocalizedString("Receipt No", selectedLanguage)
        val labelDate = getLocalizedString("Date", selectedLanguage)
        val labelItem = getLocalizedString("Ticket", selectedLanguage)
        val labelPrice = getLocalizedString("Price", selectedLanguage)
        val labelAmount = getLocalizedString("Amount", selectedLanguage)
        val labelUPI = getLocalizedString("UPI Reference No", selectedLanguage)
        val labelQty = getLocalizedString("Qty", selectedLanguage)
        val labelPhoneNumber = getLocalizedString("Phone No", selectedLanguage)
        val labelTotalAmount = getLocalizedString("Total Amount", selectedLanguage)
        val labelName = getLocalizedString("Name", selectedLanguage)
        val labelAdult = getLocalizedString("Adult", selectedLanguage)
        val labelChild = getLocalizedString("Child", selectedLanguage)
        val tempBitmap = createBitmap(width, 10000)
        val tempCanvas = Canvas(tempBitmap)

        paint.textSize = 30f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER

        val receiptTitle = when (selectedLanguage) {

            "ml" -> "പ്രവേശന ടിക്കറ്റ്"
            "kn" -> "ಪ್ರವೇಶ ಟಿಕೆಟ್"
            "ta" -> "நுழைவு டிக்கெட்"
            "te" -> "ప్రవేశ టికెట్"
            "hi" -> "प्रवेश टिकट"
            "pa" -> "ਪ੍ਰਵੇਸ਼ ਟਿਕਟ"
            "mr" -> "प्रवेश तिकीट"
            "si" -> "ප්‍රවේශ ටිකට්"
            else -> "Entry Ticket"
        }
        tempCanvas.drawText(receiptTitle, width / 2f, 40f, paint)
        var yOffset = 130f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 22f

        tempCanvas.drawText("$labelReceiptNo: ${prefix}$orderID", 20f, yOffset, paint)
        yOffset += 35f
        tempCanvas.drawText("$labelDate: $currentDate", 20f, yOffset, paint)
        yOffset += 35f
        tempCanvas.drawText("$labelName: $name", 20f, yOffset, paint)
        yOffset += 35f
        tempCanvas.drawText("$labelPhoneNumber: $phoneNo", 20f, yOffset, paint)
        yOffset += 35f


        tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
        yOffset += 40f
        paint.textAlign = Paint.Align.LEFT
        tempCanvas.drawText(labelItem, 20f, yOffset, paint)

        paint.textAlign = Paint.Align.CENTER
        tempCanvas.drawText(labelPrice, width * 0.5f, yOffset, paint)
        tempCanvas.drawText(labelQty, width * 0.65f, yOffset, paint)
        paint.textAlign = Paint.Align.RIGHT
        tempCanvas.drawText(labelAmount, width - 30f, yOffset, paint)


        yOffset += 30f
        paint.strokeWidth = 2f
        tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
        yOffset += 40f

        var totalAmount = 0.0
        val seatMapQueue = mutableMapOf<Int, MutableList<String>>()

        seatAllocations.forEach {
            seatMapQueue[it.scheduleId] = it.seats.toMutableList()
        }

        for (item in ticket) {
            val priceStr = String.format("%.2f", item.ticketRate)
            val qtyStr = if(item.ticketChild)
                item.ticketChildQty.toString()
            else
                item.ticketQty.toString()

            val qtyChildStr = item.ticketChildQty.toString()
            val priceChildStr = String.format("%.2f", item.ticketChildRate)

            val amountStr = String.format("%.2f", item.ticketTotalAmount)
            totalAmount += item.ticketTotalAmount

            paint.textAlign = Paint.Align.LEFT
            paint.isAntiAlias = true
            paint.textSize = 28f

            val itemName = when (selectedLanguage.lowercase()) {
                "ml" -> item.ticketNameMa
                "hi" -> item.ticketNameHi
                "ta" -> item.ticketNameTa
                "kn" -> item.ticketNameKa
                "te" -> item.ticketNameTe
                "si" -> item.ticketNameSi!!
                "pa" -> item.ticketNamePa
                "mr" -> item.ticketNameMr
                else -> item.ticketName
            }

            val maxItemNameWidth = width * 0.95f

            yOffset = drawMultilineText(
                canvas = tempCanvas,
                text = itemName ?: "",
                x = 20f,
                startY = yOffset,
                maxWidth = maxItemNameWidth,
                paint = paint
            ) + 10f
            paint.textSize = 22f

            if (item.ticketCombo) {

                val comboResult = activeTicketRepository.getComboResult(item.ticketId)

                val showName = comboResult.names.lastOrNull()
                val subTickets = comboResult.names.dropLast(1)
                subTickets.forEach {

                    val cleanText = it
                        .replace("\\s+".toRegex(), " ")
                        .trim()

                    if (cleanText.isNotEmpty()) {

                        val newY = drawMultilineText(
                            tempCanvas,
                            cleanText,
                            40f,
                            yOffset,
                            width * 0.9f,
                            paint
                        )

                        yOffset = newY + 2f
                    }
                }
                showName?.let { show ->
                    yOffset = drawMultilineText(
                        canvas = tempCanvas,
                        text = show,
                        x = 40f,
                        startY = yOffset,
                        maxWidth = width * 0.9f,
                        paint = paint
                    ) + 10f

                    if (item.scheduleId != 0) {

                        if (item.screenName.isNotEmpty() && item.scheduleTime.isNotEmpty()) {
                            tempCanvas.drawText(
                                item.screenName + " - " + item.scheduleTime,
                                60f,
                                yOffset,
                                paint
                            )
                            yOffset += 35f
                        }

                        val adultQty = item.ticketQty
                        val childQty = item.ticketChildQty
                        val totalQty = adultQty + childQty

                        val allSeats = seatMapQueue[item.scheduleId] ?: emptyList()

                        val adultSeats = if (adultQty > 0) {
                            allSeats.take(adultQty)
                        } else emptyList()

                        val childSeats = if (childQty > 0) {
                            allSeats.drop(adultQty).take(childQty)
                        } else emptyList()

                        var finalAdultSeats = adultSeats
                        var finalChildSeats = childSeats

                        if (finalAdultSeats.size < adultQty && finalAdultSeats.isNotEmpty()) {
                            val duplicatedSeats = mutableListOf<String>()
                            while (duplicatedSeats.size < adultQty) {
                                duplicatedSeats.addAll(finalAdultSeats)
                            }
                            finalAdultSeats = duplicatedSeats.take(adultQty)
                        }

                        if (finalChildSeats.size < childQty && finalChildSeats.isNotEmpty()) {
                            val duplicatedSeats = mutableListOf<String>()
                            while (duplicatedSeats.size < childQty) {
                                duplicatedSeats.addAll(finalChildSeats)
                            }
                            finalChildSeats = duplicatedSeats.take(childQty)
                        }

                        if (finalAdultSeats.isNotEmpty() || finalChildSeats.isNotEmpty()) {
                            paint.textSize = 28f

                            if (finalAdultSeats.isNotEmpty()) {
                                val adultSeatText = "$labelAdult Seats: ${finalAdultSeats.joinToString(", ")}"
                                yOffset = drawMultilineText(
                                    tempCanvas,
                                    adultSeatText,
                                    20f,
                                    yOffset,
                                    width * 0.9f,
                                    paint
                                ) + 10f
                            }

                            if (finalChildSeats.isNotEmpty()) {
                                val childSeatText = "$labelChild Seats: ${finalChildSeats.joinToString(", ")}"
                                yOffset = drawMultilineText(
                                    tempCanvas,
                                    childSeatText,
                                    20f,
                                    yOffset,
                                    width * 0.9f,
                                    paint
                                ) + 10f
                            }

                            // Remove used seats
                            val usedSeats = (finalAdultSeats + finalChildSeats).toSet().toList()
                            seatMapQueue[item.scheduleId]?.removeAll(usedSeats)
                        }
                    }
                }
            } else if (item.ticketType == "SHOW") {
                tempCanvas.drawText(
                    item.screenName + " - " + item.scheduleTime,
                    20f,
                    yOffset,
                    paint
                )
                yOffset += 35f

                val adultQty = item.ticketQty
                val childQty = item.ticketChildQty

                val allSeats = seatMapQueue[item.scheduleId] ?: emptyList()

                var adultSeats = if (adultQty > 0) {
                    allSeats.take(adultQty)
                } else emptyList()

                var childSeats = if (childQty > 0) {
                    allSeats.drop(adultQty).take(childQty)
                } else emptyList()

                if (adultSeats.size < adultQty && adultSeats.isNotEmpty()) {
                    val duplicatedSeats = mutableListOf<String>()
                    while (duplicatedSeats.size < adultQty) {
                        duplicatedSeats.addAll(adultSeats)
                    }
                    adultSeats = duplicatedSeats.take(adultQty)
                }

                if (childSeats.size < childQty && childSeats.isNotEmpty()) {
                    val duplicatedSeats = mutableListOf<String>()
                    while (duplicatedSeats.size < childQty) {
                        duplicatedSeats.addAll(childSeats)
                    }
                    childSeats = duplicatedSeats.take(childQty)
                }

                if (adultSeats.isNotEmpty() || childSeats.isNotEmpty()) {
                    paint.textSize = 28f

                    if (adultSeats.isNotEmpty()) {
                        val adultSeatText = "$labelAdult Seats: ${adultSeats.joinToString(", ")}"
                        yOffset = drawMultilineText(
                            tempCanvas,
                            adultSeatText,
                            20f,
                            yOffset,
                            width * 0.9f,
                            paint
                        ) + 10f
                    }

                    if (childSeats.isNotEmpty()) {
                        val childSeatText = "$labelChild Seats: ${childSeats.joinToString(", ")}"
                        yOffset = drawMultilineText(
                            tempCanvas,
                            childSeatText,
                            20f,
                            yOffset,
                            width * 0.9f,
                            paint
                        ) + 10f
                    }

                    // Remove used seats
                    val usedSeats = (adultSeats + childSeats).toSet().toList()
                    seatMapQueue[item.scheduleId]?.removeAll(usedSeats)
                }
            }

            yOffset += 35f
            paint.textSize = 22f
            paint.textAlign = Paint.Align.CENTER

            if (item.ticketQty > 0) {
                tempCanvas.drawText(priceStr, width * 0.5f, yOffset, paint)
                tempCanvas.drawText(item.ticketQty.toString(), width * 0.65f, yOffset, paint)
                yOffset += 35f
            }

            if (qtyChildStr != "0" && priceChildStr != "0" && item.ticketChildQty > 0) {
                paint.textAlign = Paint.Align.CENTER
                tempCanvas.drawText(priceChildStr, width * 0.5f, yOffset, paint)
                tempCanvas.drawText(qtyChildStr, width * 0.65f, yOffset, paint)
                yOffset += 35f
            }

            paint.textAlign = Paint.Align.RIGHT
            tempCanvas.drawText(amountStr, width - 40f, yOffset, paint)

            yOffset += 45f
        }

        paint.strokeWidth = 2f
        tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
        yOffset += 60f
        paint.textSize = 24f
        paint.textAlign = Paint.Align.RIGHT
        tempCanvas.drawText(
            "$labelTotalAmount: ${String.format("%.2f", totalAmount)}",
            width - 20f,
            yOffset,
            paint
        )
        yOffset += 25f

        if (!transID.isNullOrEmpty()) {
            paint.textSize = 18f
            paint.textAlign = Paint.Align.RIGHT
            tempCanvas.drawText(
                "$labelUPI: $transID",
                width - 20f,
                yOffset,
                paint
            )
            yOffset += 35f
        }

        if (companyRepository.getPaymentQr() == "True") {
            // Add space before QR code
            yOffset += 45f

            // Draw separator line
            paint.strokeWidth = 2f
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
            yOffset += 30f

            generateQRCode()?.let { qrBitmap ->
                val qrSize = 300
                val qrX = (width - qrSize) / 2f

                // Draw white background for better contrast
                val bgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                tempCanvas.drawRect(qrX, yOffset, qrX + qrSize, yOffset + qrSize, bgPaint)

                // Draw QR code
                tempCanvas.drawBitmap(qrBitmap, qrX, yOffset, paint)

                // Draw border
                val borderPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                tempCanvas.drawRect(qrX, yOffset, qrX + qrSize, yOffset + qrSize, borderPaint)

                yOffset += qrSize + 30f

                // Add scan instruction
                paint.textSize = 18f
                paint.textAlign = Paint.Align.CENTER
                val scanText = getLocalizedString("Scan QR Code", selectedLanguage)
                tempCanvas.drawText(scanText, width / 2f, yOffset, paint)
                yOffset += 25f

                // Draw bottom separator
                tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
                yOffset += 25f
            }
        }

        if (companyRepository.getTermAndPolicy() != null) {
            val termsAndConditions = companyRepository.getTermAndPolicy()
            if (!termsAndConditions.isNullOrEmpty()) {
                yOffset += 30f

                paint.strokeWidth = 2f
                paint.textSize = 18f
                paint.textAlign = Paint.Align.CENTER
                tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
                yOffset += 25f

                paint.textSize = 20f
                tempCanvas.drawText("TERMS & CONDITIONS", width / 2f, yOffset, paint)
                yOffset += 20f

                tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
                yOffset += 25f

                paint.textSize = 20f
                paint.textAlign = Paint.Align.LEFT

                val maxCharsPerLine = 55
                val lines = formatTermsTextForBitmap(termsAndConditions, maxCharsPerLine)

                for (line in lines) {
                    if (line.isNotEmpty()) {
                        tempCanvas.drawText(line, 20f, yOffset, paint)
                    }
                    yOffset += 25f
                }
            }
        }

        val extraPadding = if (companyRepository.getPaymentQr() == "True") 200 else 50
        val finalBitmap = createBitmap(width, (yOffset + extraPadding).toInt())
        Canvas(finalBitmap).drawBitmap(tempBitmap, 0f, 0f, null)
        tempBitmap.recycle()

        return finalBitmap
    }

    private fun formatTermsTextForBitmap(text: String, maxCharsPerLine: Int): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("")
                continue
            }

            val words = paragraph.trim().split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                if (currentLine.length + word.length + 1 <= maxCharsPerLine) {
                    if (currentLine.isNotEmpty()) {
                        currentLine.append(" ")
                    }
                    currentLine.append(word)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }

            lines.add("")
        }

        return lines
    }

    private suspend fun printLargeBitmap(printer: POSPrinter, bitmap: Bitmap) {
        val maxChunkHeight = 700
        var startY = 0

        while (startY < bitmap.height) {

            val chunkHeight = minOf(maxChunkHeight, bitmap.height - startY)

            val chunkBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                startY,
                bitmap.width,
                chunkHeight
            )

            printer.printBitmap(
                chunkBitmap,
                POSConst.ALIGNMENT_CENTER,
                600
            )

            chunkBitmap.recycle()

            startY += chunkHeight

            delay(200.milliseconds)
        }
    }

    private fun generateQRCode(): Bitmap? {
        val data = ticket.orEmpty()
        val qrCodeWriter = QRCodeWriter()
        Log.d("QRCodeDebug", ticket.toString())
        return try {
            val size = 300
            val bitMatrix: BitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = createBitmap(width, height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val color = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    bmp[x, y] = color
                }
            }
            bmp
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun generateReceiptText(
        currentDate: String,
        transID: String?,
        orderID: String?,
        ticket: List<Orders>,
        selectedLanguage: String
    ): List<String> {

        val lines = mutableListOf<String>()
        val itemWidth = 16
        val priceWidth = 6
        val qtyWidth = 4
        val amtWidth = 8
        val totalWidth = itemWidth + priceWidth + qtyWidth + amtWidth + 3
        val defaultLang = companyRepository.getDefaultLanguage().toString()

        fun String.padRight(len: Int): String =
            if (length >= len) take(len) else padEnd(len, ' ')

        fun String.padLeft(len: Int): String =
            if (length >= len) take(len) else padStart(len, ' ')


        val labelReceiptNo = getLocalizedString("Receipt No", selectedLanguage)
        val labelDate = getLocalizedString("Date", selectedLanguage)
        val labelUPI = getLocalizedString("UPI Reference No", selectedLanguage)
        val labelTotalAmount = getLocalizedString("Total Amount", selectedLanguage)
        val labelName = getLocalizedString("Name", selectedLanguage)
        val labelPhone = getLocalizedString("Phone No", selectedLanguage)
        val labelDItem = getLocalizedString("Ticket", defaultLang)
        val labelDPrice = getLocalizedString("Price", defaultLang)
        val labelDQty = getLocalizedString("Qty", defaultLang)
        val labelDAmount = getLocalizedString("Amount", defaultLang)
        val labelDTotalAmount = getLocalizedString("Total Amount", defaultLang)
        val labelDName = getLocalizedString("Name", defaultLang)
        val labelDPhone = getLocalizedString("Phone No", defaultLang)

        /* ---------- Titles ---------- */
        val receiptTitle = when (selectedLanguage) {
            "ml" -> "പ്രവേശന ടിക്കറ്റ്"
            "kn" -> "ಪ್ರವೇಶ ಟಿಕೆಟ್"
            "ta" -> "நுழைவு டிக்கெட்"
            "te" -> "ప్రవేశ టికెట్"
            "hi" -> "प्रवेश टिकट"
            "pa" -> "ਪ੍ਰਵੇਸ਼ ਟਿਕਟ"
            "mr" -> "प्रवेश तिकीट"
            "si" -> "ප්‍රවේශ ටිකට්"
            else -> "Entry Ticket"
        }

        val receiptDTitle = when (defaultLang) {
            "ml" -> "പ്രവേശന ടിക്കറ്റ്"
            "kn" -> "ಪ್ರವೇಶ ಟಿಕೆಟ್"
            "ta" -> "நுழைவு டிக்கெட்"
            "te" -> "ప్రవేశ టికెట్"
            "hi" -> "प्रवेश टिकट"
            "pa" -> "ਪ੍ਰਵੇස਼ ਟਿਕਟ"
            "mr" -> "प्रवेश तिकीट"
            "si" -> "ප්‍රවේශ ටිකට්"
            else -> "Entry Ticket"
        }

        lines.add(receiptTitle.center(45))
        lines.add(receiptDTitle.center(45))
        lines.add("")

        lines.add("**REPRINTED COPY**".center(45))
        lines.add("")

        lines.add("${labelReceiptNo.padEnd(12)}: $prefix${orderID ?: ""}")
        lines.add("${labelDate.padEnd(12)}  : $currentDate")
        lines.add("")

        lines.add(
            labelDItem.padRight(itemWidth) +
                    labelDPrice.padLeft(priceWidth) + " " +
                    labelDQty.padLeft(qtyWidth) + " " +
                    labelDAmount.padLeft(amtWidth)
        )
        lines.add("-".repeat(totalWidth))

        var totalAmount = 0.0
        for (item in ticket) {
            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.ticketRate)
            val qtyStr = item.ticketQty.toString()
            val amtStr = String.format(Locale.ENGLISH, "%.2f", item.ticketTotalAmount)
            totalAmount += item.ticketTotalAmount

            val itemName = when (selectedLanguage.lowercase()) {
                "ml" -> item.ticketNameMa
                "hi" -> item.ticketNameHi
                "ta" -> item.ticketNameTa
                "kn" -> item.ticketNameKa
                "te" -> item.ticketNameTe
                "si" -> item.ticketNameSi
                "pa" -> item.ticketNamePa
                "mr" -> item.ticketNameMr
                else -> item.ticketName
            } ?: ""

            val itemDName = when (defaultLang.lowercase()) {
                "ml" -> item.ticketNameMa
                "hi" -> item.ticketNameHi
                "ta" -> item.ticketNameTa
                "kn" -> item.ticketNameKa
                "te" -> item.ticketNameTe
                "si" -> item.ticketNameSi
                "pa" -> item.ticketNamePa
                "mr" -> item.ticketNameMr
                else -> item.ticketName
            } ?: ""

            lines.add(" ")

            itemName.chunked(totalWidth + 10).forEach { lines.add(it) }
            itemDName.chunked(totalWidth + 10).forEach { lines.add(it) }

            lines.add("")

            lines.add(
                "".padRight(itemWidth) +
                        priceStr.padLeft(priceWidth) + " " +
                        qtyStr.padLeft(qtyWidth) + " " +
                        amtStr.padLeft(amtWidth)
            )
        }
        lines.add("-".repeat(totalWidth))
        lines.add(
            labelTotalAmount.padRight(totalWidth - amtWidth) +
                    String.format(Locale.ENGLISH, "%.2f", totalAmount).padRight(amtWidth)
        )
        lines.add("($labelDTotalAmount)")

        if (!transID.isNullOrEmpty()) {
            lines.add("$labelUPI : $transID")
        }

        lines.add("$labelName : $name")
        lines.add("($labelDName)")
        lines.add("$labelPhone : $phoneNo")
        lines.add("($labelDPhone)")
        return lines
    }

    private fun generateReceiptTextDefault(
        currentDate: String,
        transID: String?,
        orderID: String?,
        ticket: List<Orders>,
        selectedLanguage: String
    ): List<String> {
        val lines = mutableListOf<String>()

        val labelReceiptNo = getLocalizedString("Receipt No", selectedLanguage)
        val labelDate = getLocalizedString("Date", selectedLanguage)
        val labelItem = getLocalizedString("Ticket", selectedLanguage)
        val labelPrice = getLocalizedString("Price", selectedLanguage)
        val labelQty = getLocalizedString("Qty", selectedLanguage)
        val labelAmount = getLocalizedString("Amount", selectedLanguage)
        val labelUPI = getLocalizedString("UPI Reference No", selectedLanguage)
        val labelTotalAmount = getLocalizedString("Total Amount", selectedLanguage)
        val labelName = getLocalizedString("Name", selectedLanguage)
        val labelPhoneNumber = getLocalizedString("Phone No", selectedLanguage)

        val receiptTitle = when (selectedLanguage) {
            "ml" -> "പ്രവേശന ടിക്കറ്റ്"
            "kn" -> "ಪ್ರವೇಶ ಟಿಕೆಟ್"
            "ta" -> "நுழைவு டிக்கெட்"
            "te" -> "ప్రవేశ టికెట్"
            "hi" -> "प्रवेश टिकट"
            "pa" -> "ਪ੍ਰਵੇਸ਼ ਟਿਕਟ"
            "mr" -> "प्रवेश तिकीट"
            "si" -> "ප්‍රවේශ ටිකට්"
            else -> "Entry Ticket"
        }

        lines.add(receiptTitle.center(45))
        lines.add("")

        lines.add("${labelReceiptNo.padEnd(12)}: $prefix${orderID ?: ""}")
        lines.add("${labelDate.padEnd(12)}: $currentDate")
        lines.add("")

        val itemColWidth = 16
        val priceColWidth = 6
        val qtyColWidth = 4
        val amountColWidth = 8
        val totalWidth = itemColWidth + priceColWidth + qtyColWidth + amountColWidth + 3
        lines.add("-".repeat(totalWidth))
        lines.add(
            labelItem.padEnd(itemColWidth) +
                    labelPrice.padStart(priceColWidth) + " " +
                    labelQty.padStart(qtyColWidth) + " " +
                    labelAmount.padStart(amountColWidth)
        )
        lines.add("-".repeat(totalWidth))

        var totalAmount = 0.0
        for (item in ticket) {
            totalAmount += item.ticketTotalAmount

            val itemName = when (selectedLanguage.lowercase()) {
                "ml" -> item.ticketNameMa
                "hi" -> item.ticketNameHi
                "ta" -> item.ticketNameTa
                "kn" -> item.ticketNameKa
                "te" -> item.ticketNameTe
                "si" -> item.ticketNameSi
                "pa" -> item.ticketNamePa
                "mr" -> item.ticketNameMr
                else -> item.ticketName
            } ?: ""

            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.ticketRate)
            val qtyStr = item.ticketRate.toString()
            val amountStr = String.format(Locale.ENGLISH, "%.2f", item.ticketTotalAmount)
            lines.add(" ")
            itemName.chunked(totalWidth + 10).forEach { lines.add(it.padEnd(itemColWidth)) }
            lines.add("")
            lines.add(
                "".padEnd(itemColWidth) +
                        priceStr.padStart(priceColWidth) + " " +
                        qtyStr.padStart(qtyColWidth) + " " +
                        amountStr.padStart(amountColWidth)
            )
        }
        lines.add("-".repeat(totalWidth))
        lines.add(
            "${labelTotalAmount.padEnd(itemColWidth + priceColWidth + qtyColWidth + 1)}: ${
                String.format(
                    Locale.ENGLISH,
                    "%.2f",
                    totalAmount
                )
            }"
        )

        if (!transID.isNullOrEmpty()) {
            lines.add("${labelUPI.padEnd(itemColWidth + priceColWidth + qtyColWidth + 1)}: $transID")
        }

        lines.add("-".repeat(totalWidth))
        lines.add("${labelName.padEnd(12)}: $name")
        lines.add("${labelPhoneNumber.padEnd(12)}: $phoneNo")
        lines.add("-".repeat(totalWidth))

        return lines
    }


    fun String.center(width: Int): String {
        if (this.length >= width) return this
        val padding = (width - this.length) / 2
        return " ".repeat(padding) + this + " ".repeat(width - this.length - padding)
    }


    private fun getLocalizedString(key: String, languageCode: String): String {
        return when (languageCode.lowercase()) {

            "en" -> key

            "ml" -> when (key) {
                "Receipt No" -> "രസീത് നമ്പർ"
                "Date" -> "തീയതി"
                "Ticket" -> "ടിക്കറ്റ്"
                "Name" -> "പേര്"
                "Phone No" -> "ഫോൺ നമ്പർ"
                "ID No" -> "ഐഡി നമ്പർ"
                "Price" -> "വില"
                "Devotees Details" -> "ഭക്തരുടെ വിശദാംശങ്ങൾ"
                "Qty" -> "അളവ്"
                "Amount" -> "തുക"
                "Total Amount" -> "ആകെ തുക"
                "UPI Reference No" -> "UPI Reference No"
                else -> key
            }


            "hi" -> when (key) {
                "Receipt No" -> "रसीद संख्या"
                "Date" -> "तारीख"
                "Ticket" -> "टिकट"
                "Name" -> "नाम"
                "Phone No" -> "फ़ोन नंबर"
                "ID No" -> "पहचान संख्या"
                "Price" -> "मूल्य"
                "Devotees Details" -> "भक्तों का विवरण"
                "Qty" -> "मात्रा"
                "Amount" -> "कुल राशि"
                "Total Amount" -> "कुल राशि"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "शीघ्र दर्शन के लिए प्रसाद नहीं है"
                else -> key
            }

            "si" -> when (key) {
                "Receipt No" -> "රිසිට්පත අංකය"
                "Date" -> "දිනය"
                "Ticket" -> "ටිකට්"
                "Name" -> "නම"
                "Phone No" -> "දුරකථන අංකය"
                "ID No" -> "හැඳුනුම් අංකය"
                "Price" -> "මිල"
                "Devotees Details" -> "භක්තයන්ගේ විස්තර"
                "Qty" -> "ප්‍රමාණය"
                "Amount" -> "මුදල"
                "Total Amount" -> "මුළු මුදල"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "ශීඝ්‍ර දර්ශනය සඳහා ප්‍රසාද නොමැත"
                else -> key
            }

            "kn" -> when (key) {
                "Receipt No" -> "ರಸೀದಿ ಸಂಖ್ಯೆ"
                "Date" -> "ದಿನಾಂಕ"
                "Ticket" -> "ಟಿಕೆಟ್"
                "Name" -> "ಹೆಸರು"
                "Phone No" -> "ಫೋನ್ ನಂಬರ"
                "ID No" -> "ಐಡಿ ಸಂಖ್ಯೆ"
                "Price" -> "ಬೆಲೆ"
                "Devotees Details" -> "ಭಕ್ತರ ವಿವರಗಳು"
                "Qty" -> "ಪ್ರಮಾಣ"
                "Amount" -> "ಮೊತ್ತ"
                "Total Amount" -> "ಒಟ್ಟು ಮೊತ್ತ"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "ಶೀಘ್ರ ದರ್ಶನಕ್ಕೆ ಪ್ರಸಾದವಿಲ್ಲ"
                else -> key
            }

            "ta" -> when (key) {
                "Receipt No" -> "ரசீது எண்"
                "Date" -> "தேதி"
                "Ticket" -> "டிக்கெட்"
                "Name" -> "பெயர்"
                "Phone No" -> "தொலைபேசி எண்"
                "ID No" -> "அடையாள எண்"
                "Price" -> "விலை"
                "Devotees Details" -> "பக்தர்களின் விவரங்கள்"
                "Qty" -> "அளவு"
                "Amount" -> "மொத்த தொகை"
                "Total Amount" -> "மொத்த தொகை"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "சீக்கிர தரிசனத்திற்கு பிரசாதம் இல்லை"
                else -> key
            }

            "te" -> when (key) {
                "Receipt No" -> "రశీదు సంఖ్య"
                "Date" -> "తేదీ"
                "Ticket" -> "టికెట్"
                "Name" -> "పేరు"
                "Phone No" -> "ఫోన్ నంబర్"
                "ID No" -> "ఐడి నంబర్"
                "Price" -> "ధర"
                "Devotees Details" -> "భక్తుల వివరాలు"
                "Qty" -> "పరిమాణం"
                "Amount" -> "మొత్తం"
                "Total Amount" -> "మొత్తం మొత్తం"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "శీఘ్ర దర్శనానికి ప్రసాదం లేదు"
                else -> key
            }

            "pa" -> when (key) {
                "Receipt No" -> "ਰਸੀਦ ਨੰਬਰ"
                "Date" -> "ਤਾਰੀਖ਼"
                "Ticket" -> "ਟਿਕਟ"
                "Name" -> "ਨਾਮ"
                "Phone No" -> "ਫ਼ੋਨ ਨੰਬਰ"
                "ID No" -> "ਆਈਡੀ ਨੰਬਰ"
                "Price" -> "ਕੀਮਤ"
                "Devotees Details" -> "ਭਗਤਾਂ ਦੇ ਵੇਰਵੇ"
                "Qty" -> "ਮਾਤਰਾ"
                "Amount" -> "ਕੁੱਲ ਰਕਮ"
                "Total Amount" -> "ਕੁੱਲ ਰਕਮ"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "ਸ਼ੀਘਰ ਦਰਸ਼ਨ ਲਈ ਪ੍ਰਸਾਦ ਨਹੀਂ ਹੈ"
                else -> key
            }

            "mr" -> when (key) {
                "Receipt No" -> "पावती क्रमांक"
                "Date" -> "तारीख"
                "Ticket" -> "तिकीट"
                "Name" -> "नाव"
                "Phone No" -> "फोन नंबर"
                "ID No" -> "ओळख क्रमांक"
                "Price" -> "किंमत"
                "Devotees Details" -> "भक्तांची माहिती"
                "Qty" -> "प्रमाण"
                "Amount" -> "एकूण रक्कम"
                "Total Amount" -> "एकूण रक्कम"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "शीघ्र दर्शनसाठी प्रसाद नाही"
                else -> key
            }

            else -> key
        }
    }

    private fun printReceiptPlutus() {
        lifecycleScope.launch {
            try {
                val message = Message.obtain(null, PlutusConstants.MESSAGE_CODE)
                val bundle = Bundle()
                val json = createPrintJson()
                Log.d("PlusRequest", json)
                bundle.putString(PlutusConstants.REQUEST_TAG, json)
                message.data = bundle
                message.replyTo = Messenger(IncomingHandler())
                serverMessenger?.send(message)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun createPrintJson(): String {
        val ticketItems = ticketRepository.getAllTicketsInCart()
        val currentDate = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH).format(Date())

        val headerBitmap = bitmapFileToHex("company_header.png", cacheDir) ?: ""
        val footerBitmap = bitmapFileToHex("company_footer.png", cacheDir) ?: ""

        val receiptLines =
            if (companyRepository.getDefaultLanguage() == selectedLanguage) {
                generateReceiptTextDefault(
                    currentDate,
                    transID,
                    orderID.toString(),
                    ticketItems,
                    selectedLanguage!!
                )
            } else {
                generateReceiptText(
                    currentDate,
                    transID,
                    orderID.toString(),
                    ticketItems,
                    selectedLanguage!!
                )
            }

        val header = JSONObject().apply {
            put("ApplicationId", "d585cf57dc5f4dab9e99fc1d37fa1333")
            put("UserId", "admin")
            put("MethodId", PlutusConstants.METHOD_PRINT)
            put("VersionNo", "1.0")
        }

        val headerImageLine = JSONObject().apply {
            put("PrintDataType", 2)
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", " ")
            put("ImagePath", "")
            put("ImageData", headerBitmap)
        }

        val footerImageLine = JSONObject().apply {
            put("PrintDataType", 2)
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", " ")
            put("ImagePath", "")
            put("ImageData", footerBitmap)

        }
        val smallSpaceLine = JSONObject().apply {
            put("PrintDataType", 0)
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", " ")
            put("ImagePath", "")
            put("ImageData", "")
        }

        val dataArray = JSONArray().apply {
            put(headerImageLine)
            /*    receiptLines.forEach { line ->

                    val cleanLine = line.replace("*", "").trim()

                    when {
                        cleanLine.equals("REPRINTED COPY", ignoreCase = true) ||
                                cleanLine.equals("Entry Ticket", ignoreCase = true) -> {

                            val bitmapText = line.trim()

                            val boldBitmapHex = centeredBoldTextToBitmapHex(bitmapText)

                            put(JSONObject().apply {
                                put("PrintDataType", 2)
                                put("PrinterWidth", 24)
                                put("IsCenterAligned", true)
                                put("DataToPrint",
                                    line.ifBlank { " " })
                                put("ImagePath", "")
                                put("ImageData", boldBitmapHex)
                            })
                        }

                        else -> {
                            put(JSONObject().apply {
                                put("PrintDataType", 0)
                                put("PrinterWidth", 200)
                                put("IsCenterAligned", false)
                                put("DataToPrint", line)
                                put("ImagePath", "")
                                put("ImageData", "")
                            })
                        }
                    }
                }*/
            put(smallSpaceLine)
            put(footerImageLine)
            put(smallSpaceLine)

        }

        val detail = JSONObject().apply {
            put("PrintRefNo", "PRN001")
            put("SavePrintData", true)
            put("Data", dataArray)
        }

        return JSONObject().apply {
            put("Header", header)
            put("Detail", detail)

        }.toString()
    }

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val response = msg.data.getString(PlutusConstants.RESPONSE_TAG)
            if (response.isNullOrEmpty()) {
                return
            }
            Log.d("PlusResponse", response)
            try {
                val json = JSONObject(response)
                val status = json.optString("Status")
                if (status.equals("SUCCESS", true)) {
                    binding.progressBar.visibility = View.GONE
                    binding.txtPrinting.visibility = View.GONE
                    redirect()
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.txtPrinting.visibility = View.GONE
                    redirect()
                }
            } catch (_: Exception) {

            }
        }
    }

    private fun centeredBoldTextToBitmapHex(
        text: String,
        bitmapWidth: Int = 384,
        textSize: Float = 30f
    ): String {

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
        }

        val fm = paint.fontMetrics
        val height = (fm.bottom - fm.top + 20).toInt()

        val bitmap = createBitmap(bitmapWidth, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val textWidth = paint.measureText(text)
        val x = (bitmapWidth - textWidth) / 2f
        val y = -fm.top + 10

        canvas.drawText(text, x, y, paint)

        return bitmapToHex(bitmap)
    }

    private fun bitmapToHex(bitmap: Bitmap): String {

        val stream = ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            stream
        )

        val bytes = stream.toByteArray()

        return bytes.joinToString("") {
            "%02X".format(it)
        }
    }

    fun loadBitmapFromCache(cacheDir: File, fileName: String): Bitmap? {
        val file = File(cacheDir, fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun bitmapFileToHex(
        fileName: String,
        cacheDir: File,
        printerWidth: Int = 360,
        quality: Int = 90
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, fileName)
                if (!file.exists()) return@withContext null

                val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: return@withContext null

                val ratio = printerWidth.toFloat() / originalBitmap.width
                val newHeight = (originalBitmap.height * ratio).toInt()
                val resizedBitmap =
                    originalBitmap.scale(printerWidth, newHeight)

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)

                outputStream.toByteArray().joinToString("") { "%02X".format(it) }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {

        val words = text.split(" ")
        var line = ""
        var y = startY

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val width = paint.measureText(testLine)

            if (width > maxWidth) {
                canvas.drawText(line, x, y, paint)
                line = word
                y += paint.textSize + 8   // 🔥 IMPORTANT spacing
            } else {
                line = testLine
            }
        }

        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            y += paint.textSize + 8   // 🔥 IMPORTANT
        }

        return y   // ✅ always return NEXT SAFE Y
    }

    private fun redirect() {
        Handler(mainLooper).postDelayed({
            lifecycleScope.launch {
                ticketRepository.clearAllData()
            }
            val targetActivity = if (from == "billing") {
                BillingTicketActivity::class.java
            } else {
                LanguageActivity::class.java
            }

            val intent = Intent(applicationContext, targetActivity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }, 1000)
    }

}
