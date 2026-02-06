package com.xenia.ticket.ui.sreens.kiosk

import android.R.attr.data
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues.TAG
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
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import com.xenia.ticket.R
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.data.room.entity.Ticket
import com.xenia.ticket.databinding.ActivityPaymentBinding
import com.xenia.ticket.ui.sreens.billing.BillingTicketActivity
import com.xenia.ticket.utils.common.CommonMethod.setLocale
import com.xenia.ticket.utils.common.CompanyKey
import com.xenia.ticket.utils.common.SessionManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.nyx.printerservice.print.IPrinterService
import net.posprinter.IDeviceConnection
import net.posprinter.IPOSListener
import net.posprinter.POSConnect
import net.posprinter.POSConst
import net.posprinter.POSPrinter
import org.koin.android.ext.android.inject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.getValue
import androidx.core.graphics.set
import com.xenia.ticket.utils.common.PlutusConstants
import com.xenia.ticket.utils.common.PlutusServiceManager
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import b.f
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.convert


class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private val sessionManager: SessionManager by inject()
    private val ticketRepository: TicketRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private var curConnect: IDeviceConnection? = null
    private var prefix: String? = null
    private var status: String? = null
    private var amount: String? = null
    private var transID: String? = null
    private var name: String? = null
    private var ticket: String? = null
    private var orderID: String? = null
    private var idProof: String? = null
    private var idProofMode: String? = null
    private var phoneNo: String? = null
    private var from: String? = null
    private var selectedLanguage: String? = null
    private var isBound = false
    private var serverMessenger: Messenger? = null
    private lateinit var plutusManager: PlutusServiceManager
    private var printerService: IPrinterService? = null
    private val handler = Handler(Looper.getMainLooper())
    private val isPlutusPrinting = AtomicBoolean(false)


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
        selectedLanguage = if (from == "billing")
            sessionManager.getBillingSelectedLanguage()
        else
            sessionManager.getSelectedLanguage()

        bindPineLabsService()
        binding.txt.setOnClickListener {
            lifecycleScope.launch {
                    printReceiptPlutus2()
            }
        }
        setLocale(this@PaymentActivity, selectedLanguage)
        if (status.equals("S")) {
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

            configPrinter()
        } else {
            binding.linSuccess.visibility = View.GONE
            binding.linFailed.visibility = View.VISIBLE
            redirect()
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
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverMessenger = null
            isBound = false
            Log.d("PLUTUS", "Service disconnected")
        }
    }


    private fun printReceiptPlutus2() {

        val base64Image = BitmapFactory.decodeResource(resources, R.drawable.logo).run {
            ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.PNG, 100, it) }
                .toByteArray()
                .let { Base64.encodeToString(it, Base64.NO_WRAP) }
        }


        val request = JSONObject().apply {
            put("Header", JSONObject().apply {
                put("ApplicationId", "d585cf57dc5f4dab9e99fc1d37fa1333")
                put("UserId", "cashier1")
                put("MethodId", PlutusConstants.METHOD_PRINT)
                put("VersionNo", "1.0")
            })

            put("Detail", JSONObject().apply {
                put("PrintRefNo", "PR12345")
                put("SavePrintData", true)
                put("Data", base64Image)
            })
        }
        Log.e("PLUTUS_RESP", request.toString())
        plutusManager.sendRequest(request.toString())
    }
    private fun configPrinter() {
        val selectedPrinter = sessionManager.getSelectedPrinter()
        when (selectedPrinter) {
            "B200MAX" -> {
                bindAndPrintSale()
            }

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
                        Toast.makeText(this, "No USB printer devices found", Toast.LENGTH_LONG)
                            .show()
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

    private fun bindAndPrintSale() {
//        if (isBound) {
//            initReceiptPrint(true)
//            return
//        }
//
//        val intent = Intent().apply {
//            `package` = "com.incar.printerservice"
//            action = "com.incar.printerservice.IPrinterService"
//        }
//
//        serviceConnection = object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                printerService = IPrinterService.Stub.asInterface(service)
//                isBound = true
//                Log.d(TAG, "Printer service connected")
//                initReceiptPrint(true)
//            }
//
//            override fun onServiceDisconnected(name: ComponentName?) {
//                printerService = null
//                isBound = false
//                Log.e(TAG, "Printer service disconnected")
//                handler.postDelayed({
//                    initReceiptPrint(true)
//                }, 5000)
//            }
//        }
//
//        applicationContext.bindService(intent, serviceConnection!!, BIND_AUTO_CREATE)
    }

    private val connectListener = IPOSListener { code, _ ->

        val selectedPrinter = sessionManager.getSelectedPrinter()

        when (code) {
            POSConnect.CONNECT_SUCCESS -> {
                if (selectedPrinter == "FALCON" || selectedPrinter == "KIOSK" || selectedPrinter == null) {
                    initReceiptPrint(false)
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
    private fun initReceiptPrint(isB1008: Boolean) {
        lifecycleScope.launch {
            val allVazhipaduItems = ticketRepository.getAllTicketsInCart()
            val currentDate = SimpleDateFormat(
                "dd-MMM-yyyy hh:mm a",
                Locale.ENGLISH
            ).format(Date())

            val headerUrl = companyRepository.getString(CompanyKey.COMPANYPRINT_H)
            val footerUrl = companyRepository.getString(CompanyKey.COMPANYPRINT_F)

            Log.d("ReceiptPrint", "Full Header URL: $headerUrl")
            Log.d("ReceiptPrint", "Full Footer URL: $footerUrl")

            val (headerBitmap, footerBitmap) = withContext(Dispatchers.IO) {
                val header = headerUrl?.let { safeLoadBitmap(it) }
                val footer = footerUrl?.let { safeLoadBitmap(it) }
                header to footer
            }
            val receiptBitmap: Bitmap =
                if (companyRepository.getDefaultLanguage() == selectedLanguage) {
                    generateReceiptBitmapDefault(
                        currentDate,
                        transID,
                        orderID.toString(),
                        allVazhipaduItems,
                        selectedLanguage!!
                    )
                } else {
                    generateReceiptBitmap(
                        currentDate,
                        transID,
                        orderID.toString(),
                        allVazhipaduItems,
                        selectedLanguage!!
                    )
                }

            if (isB1008) {
                try {
                    headerBitmap?.scale(550, 200)?.let { scaled ->
                        printerService?.printBitmap(scaled, 0, POSConst.ALIGNMENT_CENTER)
                        scaled.recycle()
                    }

                    printerService?.printBitmap(receiptBitmap, 0, POSConst.ALIGNMENT_CENTER)
                    printerService?.printText("\n\n", null)

                    footerBitmap?.scale(500, 100)?.let { scaled ->
                        printerService?.printBitmap(scaled, 0, POSConst.ALIGNMENT_CENTER)
                        scaled.recycle()
                    }

                    printerService?.printEndAutoOut()
                } catch (e: RemoteException) {
                    Log.e("PrinterService", "Printing error: ${e.message}")
                }

            } else {
                val printer = POSPrinter(curConnect)

                headerBitmap?.scale(550, 200)?.let { scaled ->
                    printer.printBitmap(scaled, POSConst.ALIGNMENT_CENTER, 500).feedLine(2)
                    scaled.recycle()
                }

                printer.printBitmap(receiptBitmap, POSConst.ALIGNMENT_CENTER, 600).feedLine(2)
                delay(100)

                try {
                    footerBitmap?.scale(550, 100)?.let { scaled ->
                        printer.printBitmap(scaled, POSConst.ALIGNMENT_CENTER, 500)
                        printer.feedLine(3)
                        delay(300)
                        printer.cutHalfAndFeed(1)
                        scaled.recycle()
                    } ?: printer.cutHalfAndFeed(1)
                } catch (e: Exception) {
                    Log.e("ReceiptPrint", "Footer printing error: ${e.message}")
                    printer.cutHalfAndFeed(1)
                }
            }

            receiptBitmap.recycle()
            delay(2000)

            redirect()
        }
    }



    private suspend fun printReceiptPlutus() {
        lifecycleScope.launch {

            try {
                val message = Message.obtain(null, PlutusConstants.MESSAGE_CODE)
                val bundle = Bundle()

                val json = createPrintJson()

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
        val allVazhipaduItems = ticketRepository.getAllTicketsInCart()

        val currentDate = SimpleDateFormat(
            "dd-MMM-yyyy hh:mm a",
            Locale.ENGLISH
        ).format(Date())

        val headerBitmap = bitmapFileToHex("company_header.png", cacheDir) ?: ""
        val footerBitmap = bitmapFileToHex("company_footer.png", cacheDir) ?: ""

        val receiptLines =
            if (companyRepository.getDefaultLanguage() == selectedLanguage) {
                generateReceiptTextDefault(
                    currentDate,
                    transID,
                    orderID.toString(),
                    allVazhipaduItems,
                    selectedLanguage!!
                )
            } else {
                generateReceiptText(
                    currentDate,
                    transID,
                    orderID.toString(),
                    allVazhipaduItems,
                    selectedLanguage!!
                )
            }

        val header = JSONObject().apply {
            put("ApplicationId", sessionManager.getPineLabsAppId())
            put("UserId", "admin")
            put("MethodId", PlutusConstants.METHOD_PRINT)
            put("VersionNo", "1.0")
        }

        // Header image
        val headerImageLine = JSONObject().apply {
            put("PrintDataType", 2) // 2 = image
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", "")
            put("ImagePath", "")
            put("ImageData", headerBitmap)
        }

        // Footer image
        val footerImageLine = JSONObject().apply {
            put("PrintDataType", 2) // 2 = image
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", "")
            put("ImagePath", "")
            put("ImageData", footerBitmap)

        }
        val smallSpaceLine = JSONObject().apply {
            put("PrintDataType", 0) // 2 = image
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", " ")
            put("ImagePath", "")
            put("ImageData", "")
        }
        val largeSpaceLine = JSONObject().apply {
            put("PrintDataType", 0)
            put("PrinterWidth", 24)
            put("IsCenterAligned", false)
            put("DataToPrint", "\n")
            put("ImagePath", "")
            put("ImageData", "")
        }

        val dataArray = JSONArray().apply {
            put(headerImageLine)
            receiptLines.forEach { line ->
                put(JSONObject().apply {
                    put("PrintDataType", 0)
                    put("PrinterWidth", 200)
                    put("IsCenterAligned", false)
                    put("DataToPrint", line)
                    put("ImagePath", "")
                    put("ImageData", "")
                })
            }
            put(smallSpaceLine)
            put(footerImageLine)
            put(largeSpaceLine)
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
    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {

            val response = msg.data.getString(PlutusConstants.RESPONSE_TAG)
            Log.d("PLUTUS_RESPONSE", response ?: "NULL")

            if (response.isNullOrEmpty()) {
                Toast.makeText(this@PaymentActivity, "No response from printer", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                val json = JSONObject(response)
                val status = json.optString("Status")
                val message = json.optString("Message")

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
                Toast.makeText(this@PaymentActivity, "Invalid printer response", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun safeLoadBitmap(url: String): Bitmap? = try {
        loadBitmapFromUrl(url)
    } catch (e: Exception) {
        Log.e("ReceiptPrint", "Error loading bitmap from $url: ${e.message}")
        null
    }

    private suspend fun loadBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val bitmap = BitmapFactory.decodeStream(connection.inputStream)
            connection.inputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.e("ImageLoad", "Error loading image from URL: ${e.message}")
            null
        }
    }


    @SuppressLint("DefaultLocale")
    private suspend fun generateReceiptBitmap(
        currentDate: String,
        transID: String?,
        orderID: String?,
        ticket: List<Ticket>,
        selectedLanguage: String
    ): Bitmap {
        val width = 576
        val paint = Paint().apply { isAntiAlias = true }
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
        val labelDReceiptNo = getLocalizedString("Receipt No", defaultLang)
        val labelDDate = getLocalizedString("Date", defaultLang)
        val labelDPhonenumber = getLocalizedString("Phone No", defaultLang)
        val labelDTotalAmount = getLocalizedString("Total Amount", defaultLang)
        val labelDName = getLocalizedString("Name", defaultLang)
        val labelDItem = getLocalizedString("Ticket", defaultLang)
        val labelDPrice = getLocalizedString("Price", defaultLang)
        val labelDAmount = getLocalizedString("Amount", defaultLang)
        val labelDQty = getLocalizedString("Qty", defaultLang)

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
        tempCanvas.drawText(receiptDTitle, width / 2f, 80f, paint)

        var yOffset = 130f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 22f

        tempCanvas.drawText(
            "$labelReceiptNo($labelDReceiptNo): ${prefix}$orderID",
            20f,
            yOffset,
            paint
        )
        yOffset += 35f
        tempCanvas.drawText("$labelDate($labelDDate): $currentDate", 20f, yOffset, paint)
        yOffset += 35f

        yOffset += 30f
        paint.textAlign = Paint.Align.LEFT
        tempCanvas.drawText(labelItem, 20f, yOffset, paint)
        paint.textAlign = Paint.Align.CENTER
        tempCanvas.drawText(labelPrice, width * 0.5f, yOffset, paint)
        tempCanvas.drawText(labelQty, width * 0.65f, yOffset, paint)
        paint.textAlign = Paint.Align.RIGHT
        tempCanvas.drawText(labelAmount, width - 30f, yOffset, paint)
        yOffset += 30f
        paint.textAlign = Paint.Align.LEFT
        tempCanvas.drawText(labelDItem, 20f, yOffset, paint)
        paint.textAlign = Paint.Align.CENTER
        tempCanvas.drawText(labelDPrice, width * 0.5f, yOffset, paint)
        tempCanvas.drawText(labelDQty, width * 0.65f, yOffset, paint)
        paint.textAlign = Paint.Align.RIGHT
        tempCanvas.drawText(labelDAmount, width - 30f, yOffset, paint)
        yOffset += 30f
        paint.strokeWidth = 2f
        tempCanvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)
        yOffset += 40f

        var totalAmount = 0.0
        for (item in ticket) {
            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.daRate)
            val qtyStr = item.daQty.toString()
            val amountStr = String.format(Locale.ENGLISH, "%.2f", item.daTotalAmount)
            totalAmount += item.daTotalAmount

            paint.textAlign = Paint.Align.LEFT
            paint.isAntiAlias = true


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
            val itemDName = when (defaultLang.lowercase()) {
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

            yOffset += 15f

            yOffset = drawMultilineText(
                canvas = tempCanvas,
                text = itemDName ?: "",
                x = 20f,
                startY = yOffset,
                maxWidth = maxItemNameWidth,
                paint = paint
            ) + 10f

            yOffset += 35f
            paint.textAlign = Paint.Align.CENTER
            tempCanvas.drawText(priceStr, width * 0.5f, yOffset, paint)
            tempCanvas.drawText(qtyStr, width * 0.65f, yOffset, paint)
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
            "$labelTotalAmount ($labelDTotalAmount): ${
                String.format(
                    Locale.ENGLISH,
                    "%.2f",
                    totalAmount
                )
            }",
            width - 20f,
            yOffset,
            paint
        )
        yOffset += 30f

        if (!transID.isNullOrEmpty()) {
            paint.textSize = 18f
            paint.textAlign = Paint.Align.RIGHT
            tempCanvas.drawText(
                "$labelUPI: $transID",
                width - 20f,
                yOffset,
                paint
            )
            yOffset += 25f
        }

        yOffset += 35f
        paint.textSize = 22f
        paint.textAlign = Paint.Align.CENTER


        val padding = 20f
        val textPadding = 20f
        val rectTop = yOffset
        val rectRight = width - padding

        var tempYOffset = yOffset + textPadding

        tempYOffset += 20f
        val imageSize = 100f
        val textHeight = 35f * 3
        tempYOffset += maxOf(imageSize, textHeight) + 20f
        val rectBottom = tempYOffset + textPadding
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        val innerPadding = 30f
        paint.strokeWidth = 1f
        val cornerRadius = 20f

        tempCanvas.drawRoundRect(
            padding, rectTop, rectRight, rectBottom,
            cornerRadius, cornerRadius,
            paint
        )

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT
        val textX = padding + innerPadding
        var textY = rectTop + innerPadding - paint.fontMetrics.ascent
        tempCanvas.drawText("$labelName : $name", textX, textY, paint)
        textY += 25f
        tempCanvas.drawText(labelDName, textX, textY, paint)
        textY += 35f
        tempCanvas.drawText("$labelPhoneNumber: $phoneNo", textX, textY, paint)
        textY += 25f
        tempCanvas.drawText(labelDPhonenumber, textX, textY, paint)
        yOffset = textY + 60f

        if (from.isNullOrEmpty() || from != "billing") {
            generateQRCode()?.let { qrBitmap ->
                val qrSize = 300
                val qrX = (width - qrSize) / 2f
                tempCanvas.drawBitmap(qrBitmap, qrX, yOffset, paint)
                yOffset += qrSize
            }
        }
        val finalBitmap = createBitmap(width, (yOffset + 20f).toInt())
        Canvas(finalBitmap).drawBitmap(tempBitmap, 0f, 0f, null)
        tempBitmap.recycle()

        return finalBitmap
    }

    @SuppressLint("DefaultLocale")
    private fun generateReceiptBitmapDefault(
        currentDate: String,
        transID: String?,
        orderID: String?,
        ticket: List<Ticket>,
        selectedLanguage: String
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


        yOffset += 30f
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
        for (item in ticket) {
            val priceStr = String.format("%.2f", item.daRate)
            val qtyStr = item.daQty.toString()
            val amountStr = String.format("%.2f", item.daTotalAmount)
            totalAmount += item.daTotalAmount

            paint.textAlign = Paint.Align.LEFT
            paint.isAntiAlias = true


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

            yOffset += 35f

            paint.textAlign = Paint.Align.CENTER
            tempCanvas.drawText(priceStr, width * 0.5f, yOffset, paint)
            tempCanvas.drawText(qtyStr, width * 0.65f, yOffset, paint)
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

        yOffset += 35f
        val padding = 15f
        val textPadding = 0f
        val rectTop = yOffset
        val rectRight = width - padding
        var tempYOffset = yOffset + textPadding
        tempYOffset += 10f
        val imageSize = 100f
        val textHeight = 35f * 3
        tempYOffset += maxOf(imageSize, textHeight) + 20f

        val rectBottom = tempYOffset + textPadding
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        val innerPadding = 30f
        val cornerRadius = 20f
        paint.strokeWidth = 1.5f
        tempCanvas.drawRoundRect(
            padding, rectTop, rectRight, rectBottom,
            cornerRadius, cornerRadius,
            paint
        )
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT
        val textX = padding + innerPadding
        var textY = rectTop + innerPadding - paint.fontMetrics.ascent
        tempCanvas.drawText("$labelName : $name", textX, textY, paint)
        textY += 30f
        tempCanvas.drawText("$labelPhoneNumber: $phoneNo", textX, textY, paint)

        yOffset = textY + 60f

        if (from.isNullOrEmpty() || from != "billing") {
            generateQRCode()?.let { qrBitmap ->
                val qrSize = 300
                val qrX = (width - qrSize) / 2f
                tempCanvas.drawBitmap(qrBitmap, qrX, yOffset, paint)
                yOffset += qrSize
            }
        }
        val finalBitmap = createBitmap(width, (yOffset + 10f).toInt())
        Canvas(finalBitmap).drawBitmap(tempBitmap, 0f, 0f, null)
        tempBitmap.recycle()
        return finalBitmap
    }

    private fun generateQRCode(): Bitmap? {
        val data = ticket.orEmpty()
        val qrCodeWriter = QRCodeWriter()
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
        ticket: List<Ticket>,
        selectedLanguage: String
    ): List<String> {

        val lines = mutableListOf<String>()
        val LINE_WIDTH = 45  // Changed to match first function's center width
        val ITEM_WIDTH = 16  // Changed to match first function
        val PRICE_WIDTH = 6  // Changed to match first function
        val QTY_WIDTH = 4    // Changed to match first function
        val AMT_WIDTH = 8    // Changed to match first function
        val totalWidth = ITEM_WIDTH + PRICE_WIDTH + QTY_WIDTH + AMT_WIDTH + 3  // ~37, for dashes
        val defaultLang = companyRepository.getDefaultLanguage().toString()

        fun String.padRight(len: Int): String =
            if (length >= len) take(len) else padEnd(len, ' ')

        fun String.padLeft(len: Int): String =
            if (length >= len) take(len) else padStart(len, ' ')

        fun center(text: String): String {
            val pad = (LINE_WIDTH - text.length) / 2
            return " ".repeat(pad.coerceAtLeast(0)) + text
        }

        /* ---------- Labels ---------- */
        val labelReceiptNo = getLocalizedString("Receipt No", selectedLanguage)
        val labelDate = getLocalizedString("Date", selectedLanguage)
        val labelItem = getLocalizedString("Ticket", selectedLanguage)
        val labelPrice = getLocalizedString("Price", selectedLanguage)
        val labelQty = getLocalizedString("Qty", selectedLanguage)
        val labelAmount = getLocalizedString("Amount", selectedLanguage)
        val labelUPI = getLocalizedString("UPI Reference No", selectedLanguage)
        val labelTotalAmount = getLocalizedString("Total Amount", selectedLanguage)
        val labelName = getLocalizedString("Name", selectedLanguage)
        val labelPhone = getLocalizedString("Phone No", selectedLanguage)

        val labelDReceiptNo = getLocalizedString("Receipt No", defaultLang)
        val labelDDate = getLocalizedString("Date", defaultLang)
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

        // Header - Changed to match first function's style
        lines.add(receiptTitle.center(45))
        lines.add(receiptDTitle.center(45))
        lines.add("")

        lines.add("${labelReceiptNo.padEnd(12)}: ${orderID ?: ""}")
        lines.add("${labelDate.padEnd(12)}  : $currentDate")
        lines.add("")

        lines.add(
            labelDItem.padRight(ITEM_WIDTH) +
                    labelDPrice.padLeft(PRICE_WIDTH) + " " +
                    labelDQty.padLeft(QTY_WIDTH) + " " +
                    labelDAmount.padLeft(AMT_WIDTH)
        )
        lines.add("-".repeat(totalWidth))

        var totalAmount = 0.0

        for (item in ticket) {
            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.daRate)
            val qtyStr = item.daQty.toString()
            val amtStr = String.format(Locale.ENGLISH, "%.2f", item.daTotalAmount)
            totalAmount += item.daTotalAmount

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

            // Add a blank line before each item
            lines.add(" ")

            // Add all chunks of the item name first
            itemName.chunked(totalWidth+10).forEach { lines.add(it) }
            itemDName.chunked(totalWidth+10).forEach { lines.add(it) }

            // Added extra space between item name and rates
            lines.add("")

            // Then add the price, qty, and amount on the next line
            lines.add(
                "".padRight(ITEM_WIDTH) +
                        priceStr.padLeft(PRICE_WIDTH) + " " +
                        qtyStr.padLeft(QTY_WIDTH) + " " +
                        amtStr.padLeft(AMT_WIDTH)
            )
        }
        lines.add("-".repeat(totalWidth))  // Changed to totalWidth
        lines.add(
            labelTotalAmount.padRight(totalWidth - AMT_WIDTH) +  // Adjusted for totalWidth
                    String.format(Locale.ENGLISH, "%.2f", totalAmount).padRight(AMT_WIDTH)
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
        ticket: List<Ticket>,
        selectedLanguage: String
    ): List<String> {
        val lines = mutableListOf<String>()

        // Localized labels
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

        // Receipt title
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

        // Header
        lines.add(receiptTitle.center(45))
        lines.add("")

        lines.add("${labelReceiptNo.padEnd(12)}: ${orderID ?: ""}")
        lines.add("${labelDate.padEnd(12)}: $currentDate")
        lines.add("")

        val itemColWidth = 16
        val priceColWidth = 6
        val qtyColWidth = 4
        val amountColWidth = 8
        val totalWidth = itemColWidth + priceColWidth + qtyColWidth + amountColWidth + 3

        lines.add(
            labelItem.padEnd(itemColWidth) +
                    labelPrice.padStart(priceColWidth) + " " +
                    labelQty.padStart(qtyColWidth) + " " +
                    labelAmount.padStart(amountColWidth)
        )
        lines.add("-".repeat(totalWidth))

        var totalAmount = 0.0
        for (item in ticket) {
            totalAmount += item.daTotalAmount

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

            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.daRate)
            val qtyStr = item.daQty.toString()
            val amountStr = String.format(Locale.ENGLISH, "%.2f", item.daTotalAmount)
            lines.add(" ")
            itemName.chunked(totalWidth+10).forEach { lines.add(it.padEnd(itemColWidth)) }
           lines.add("")
            lines.add(
                "".padEnd(itemColWidth) +
                        priceStr.padStart(priceColWidth) + " " +
                        qtyStr.padStart(qtyColWidth) + " " +
                        amountStr.padStart(amountColWidth)
            )
        }
        lines.add("-".repeat(totalWidth))
        lines.add("${labelTotalAmount.padEnd(itemColWidth + priceColWidth + qtyColWidth + 1)}: ${String.format(Locale.ENGLISH, "%.2f", totalAmount)}")

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

                // Resize to printer width
                val ratio = printerWidth.toFloat() / originalBitmap.width
                val newHeight = (originalBitmap.height * ratio).toInt()
                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, printerWidth, newHeight, true)

                // Convert to HEX
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)

                outputStream.toByteArray().joinToString("") { "%02X".format(it) }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
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

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineSpacing: Float = 8f
    ): Float {
        val words = text.split(" ")
        var line = ""
        var y = startY
        val lineHeight = paint.fontMetrics.run { descent - ascent }

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) <= maxWidth) {
                line = testLine
            } else {
                canvas.drawText(line, x, y, paint)
                y += lineHeight + lineSpacing
                line = word
            }
        }

        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }

        return y
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
