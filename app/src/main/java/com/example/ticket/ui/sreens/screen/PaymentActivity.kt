package com.example.ticket.ui.sreens.screen

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.set
import androidx.lifecycle.lifecycleScope
import com.example.ticket.R
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.databinding.ActivityPaymentBinding
import com.example.ticket.utils.common.AESEncryptionUtil
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.Constants.SECRET_KEY
import com.example.ticket.utils.common.SessionManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.urovo.sdk.print.PrinterProviderImpl
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


class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private val sessionManager: SessionManager by inject()
    private val ticketRepository: TicketRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private var curConnect: IDeviceConnection? = null

    private var status: String? = null
    private var amount: String? = null
    private var transID: String? = null
    private var name: String? = null
    private var orderID: Long = 0L
    private var idProof: String? = null
    private var idProofMode: String? = null
    private var phoneNo: String? = null
    private var from: String? = null
    private var selectedLanguage: String? = null
    private var isBound = false

    private var printerService: IPrinterService? = null
    private var serviceConnection: ServiceConnection? = null
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        from = intent.getStringExtra("from")
        status = intent.getStringExtra("status")
        amount = intent.getStringExtra("amount")
        transID = intent.getStringExtra("transID")
        orderID = intent.getLongExtra("orderID", 0L)
        phoneNo = intent.getStringExtra("phno")
        idProof = intent.getStringExtra("IDNO")
        idProofMode = intent.getStringExtra("ID")
        name = intent.getStringExtra("name")
        selectedLanguage = if (from == "billing")
            sessionManager.getBillingSelectedLanguage()
        else
            sessionManager.getSelectedLanguage()

        setLocale(this@PaymentActivity, selectedLanguage)
        if (status.equals("S")) {
            binding.linSuccess.visibility = View.VISIBLE
            binding.linFailed.visibility = View.GONE
            val amountDouble = amount?.toDoubleOrNull() ?: 0.00
            val formattedAmount = String.format("%.2f", amountDouble)
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
                finish() // Optional: close current activity
            }
        }
    }

    private fun bindAndPrintSale() {
        if (isBound) {
            initReceiptPrint(true)
            return
        }

        val intent = Intent().apply {
            `package` = "com.incar.printerservice"
            action = "com.incar.printerservice.IPrinterService"
        }

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                printerService = IPrinterService.Stub.asInterface(service)
                isBound = true
                Log.d(TAG, "Printer service connected")
                initReceiptPrint(true)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                printerService = null
                isBound = false
                Log.e(TAG, "Printer service disconnected")
                handler.postDelayed({
                    initReceiptPrint(true)
                }, 5000)
            }
        }

        applicationContext.bindService(intent, serviceConnection!!, BIND_AUTO_CREATE)
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

            val headerFileName = companyRepository.getString(CompanyKey.COMPANYPRINT_H)
            val footerFileName = companyRepository.getString(CompanyKey.COMPANYPRINT_F)

            val (headerBitmap, footerBitmap) = withContext(Dispatchers.IO) {
                val headerUrl =
                    headerFileName?.let { "https://apiimage.xeniapos.com/Temple/assest/uploads?fileName=$it" }
                val footerUrl =
                    footerFileName?.let { "https://apiimage.xeniapos.com/Temple/assest/uploads?fileName=$it" }


                Log.d("ReceiptPrint", "Header URL: $headerUrl")
                Log.d("ReceiptPrint", "Footer URL: $footerUrl")

                val header = headerUrl?.let {
                    try {
                        loadBitmapFromUrl(it)
                    } catch (e: Exception) {
                        Log.e("ReceiptPrint", "Error loading header bitmap: ${e.message}")
                        null
                    }
                }

                val footer = footerUrl?.let {
                    try {
                        loadBitmapFromUrl(it)
                    } catch (e: Exception) {
                        Log.e("ReceiptPrint", "Error loading footer bitmap: ${e.message}")
                        null
                    }
                }

                header to footer
            }


            val receiptBitmap: Bitmap = when {
                companyRepository.getDefaultLanguage() == selectedLanguage -> {
                    generateReceiptBitmapDefault(
                        currentDate,
                        transID,
                        orderID.toString(),
                        allVazhipaduItems,
                        selectedLanguage!!
                    )
                }

                else -> {
                    generateReceiptBitmap(
                        currentDate,
                        transID,
                        orderID.toString(),
                        allVazhipaduItems,
                        selectedLanguage!!
                    )

                }
            }

            if (isB1008) {
                try {
                    headerBitmap?.let { bmp ->
                        val scaledHeader = bmp.scale(550, 200)
                        printerService?.printBitmap(
                            scaledHeader,
                            0, // type
                            POSConst.ALIGNMENT_CENTER
                        )
                        scaledHeader.recycle()
                    }

                    printerService?.printBitmap(
                        receiptBitmap,
                        0,
                        POSConst.ALIGNMENT_CENTER
                    )
                    printerService?.printText("\n\n", null)

                    footerBitmap?.let { bmp ->
                        val scaledFooter = bmp.scale(500, 100)
                        printerService?.printBitmap(
                            scaledFooter,
                            0,
                            POSConst.ALIGNMENT_CENTER
                        )
                        scaledFooter.recycle()
                    }

                    printerService!!.printEndAutoOut()
                } catch (e: RemoteException) {
                    Log.e("PrinterService", "Printing error: ${e.message}")
                }

            } else {
                val printer = POSPrinter(curConnect)


                headerBitmap?.let {
                    val compressedHeader = it.scale(550, 200)
                    printer.printBitmap(compressedHeader, POSConst.ALIGNMENT_CENTER, 500)
                        .feedLine(2)
                    compressedHeader.recycle()
                }

                printer.printBitmap(receiptBitmap, POSConst.ALIGNMENT_CENTER, 600)
                    .feedLine(2)

                delay(100)

                try {
                    footerBitmap?.let {
                        val compressedFooter = it.scale(550, 100)
                        printer.printBitmap(compressedFooter, POSConst.ALIGNMENT_CENTER, 500)
                        printer.feedLine(3)
                        delay(300)
                        printer.cutHalfAndFeed(1)

                        compressedFooter.recycle()
                    } ?: run {
                        printer.cutHalfAndFeed(1)
                    }
                } catch (e: Exception) {
                    Log.e("ReceiptPrint", "Footer printing error: ${e.message}")
                    printer.cutHalfAndFeed(1)
                }
            }

            receiptBitmap.recycle()
            delay(2000)
        }

        redirect()
    }

    private suspend fun loadBitmapFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.e("ImageLoad", "Error loading image from URL: ${e.message}")
            null
        }
    }

    private fun Bitmap.scaleToWidth(newWidth: Int): Bitmap {
        val ratio = newWidth.toFloat() / this.width
        val newHeight = (this.height * ratio).toInt()
        return this.scale(newWidth, newHeight)
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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK

        val tempBitmap = Bitmap.createBitmap(width, 5000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)

        var yOffset = 40f


        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
        paint.isFakeBoldText = true
        canvas.drawText("THRISSUR ZOOLOGICAL PARK", width / 2f, yOffset, paint)

        yOffset += 35f
        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Puthur, Thrissur, Kerala 680014",
            width / 2f,
            yOffset,
            paint
        )

        yOffset += 35f
        paint.textSize = 22f
        canvas.drawText("Thrissur Zoological Park", width / 2f, yOffset, paint)



        yOffset += 45f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f

        canvas.drawText("Receipt No : $orderID", 20f, yOffset, paint)
        yOffset += 30f
        canvas.drawText(
            "Date : ${currentDate.replace("-", " - ")}",
            20f,
            yOffset,
            paint
        )


        yOffset += 60f
        paint.textSize = 22f

        canvas.drawText("Item", 20f, yOffset, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Price", width * 0.50f, yOffset, paint)
        canvas.drawText("Qty", width * 0.65f, yOffset, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", width - 20f, yOffset, paint)

        yOffset += 15f
        canvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)


        yOffset += 30f
        var totalAmount = 0.0

        for (item in ticket) {
            totalAmount += item.daTotalAmount

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(item.ticketName ?: "", 20f, yOffset, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(item.daRate.toInt().toString(), width * 0.50f, yOffset, paint)
            canvas.drawText(item.daQty.toString(), width * 0.65f, yOffset, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "₹${item.daTotalAmount.toInt()}",
                width - 20f,
                yOffset,
                paint
            )

            yOffset += 50f
        }

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 26f
        paint.isFakeBoldText = true

        canvas.drawText(
            "Total Amount : ₹${totalAmount.toInt()}",
            width - 20f,
            yOffset,
            paint
        )


        paint.isFakeBoldText = false


        yOffset += 40f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        val boxTop = yOffset
        val boxBottom = boxTop + 200f

        canvas.drawRoundRect(
            20f,
            boxTop,
            width - 20f,
            boxBottom,
            20f,
            20f,
            paint
        )

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 22f

        canvas.drawText("Visitor Details", width / 2f, boxTop + 35f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f

        canvas.drawText("Name : $name", 40f, boxTop + 75f, paint)
        canvas.drawText("Phone No : $phoneNo", 40f, boxTop + 110f, paint)
        canvas.drawText("ID No : $idProof", 40f, boxTop + 145f, paint)

        yOffset = boxBottom + 40f


        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 20f
        canvas.drawText("Thank You!", width / 2f, yOffset, paint)

        yOffset += 30f
        paint.textSize = 16f
        canvas.drawText("Powered by www.xenionline.in", width / 2f, yOffset, paint)



        val finalBitmap =
            Bitmap.createBitmap(width, (yOffset + 40f).toInt(), Bitmap.Config.ARGB_8888)
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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }


        val labelReceiptNo = getLocalizedString("Receipt No", selectedLanguage)
        val labelDate = getLocalizedString("Date", selectedLanguage)
        val labelItem = getLocalizedString("Item", selectedLanguage)
        val labelPrice = getLocalizedString("Price", selectedLanguage)
        val labelQty = getLocalizedString("Qty", selectedLanguage)
        val labelAmount = getLocalizedString("Amount", selectedLanguage)
        val labelTotalAmount = getLocalizedString("Total Amount", selectedLanguage)
        val labelVisitorDetails = getLocalizedString("Visitor Details", selectedLanguage)
        val labelName = getLocalizedString("Name", selectedLanguage)
        val labelPhonenumber = getLocalizedString("Phone No", selectedLanguage)
        val labelIdNo = getLocalizedString("ID No", selectedLanguage)

        val tempBitmap = Bitmap.createBitmap(width, 6000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)

        var yOffset = 40f

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
        paint.isFakeBoldText = true
        canvas.drawText("THRISSUR ZOOLOGICAL PARK", width / 2f, yOffset, paint)

        yOffset += 35f
        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Puthur, Thrissur, Kerala 680014",
            width / 2f,
            yOffset,
            paint
        )

        yOffset += 35f
        paint.textSize = 22f
        canvas.drawText("Thrissur Zoological Park", width / 2f, yOffset, paint)



        yOffset += 45f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f

        canvas.drawText("$labelReceiptNo : $orderID", 20f, yOffset, paint)
        yOffset += 30f
        canvas.drawText("$labelDate : $currentDate", 20f, yOffset, paint)



        yOffset += 40f
        paint.textSize = 22f

        canvas.drawText(labelItem, 20f, yOffset, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(labelPrice, width * 0.50f, yOffset, paint)
        canvas.drawText(labelQty, width * 0.65f, yOffset, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(labelAmount, width - 20f, yOffset, paint)

        yOffset += 15f
        canvas.drawLine(20f, yOffset, width - 20f, yOffset, paint)


        yOffset += 30f
        var totalAmount = 0.0

        for (item in ticket) {
            totalAmount += item.daTotalAmount

            val itemName = when (selectedLanguage.lowercase()) {
                "ml" -> item.ticketNameMa
                "hi" -> item.ticketNameHi
                "ta" -> item.ticketNameTe
                "kn" -> item.ticketNameKa
                "te" -> item.ticketNameTe
                "si" -> item.ticketNameSi
                "mr" -> item.ticketNameMr
                "pa" -> item.ticketNamePa
                else -> item.ticketName
            }

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(itemName ?: "", 20f, yOffset, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(item.daRate.toInt().toString(), width * 0.50f, yOffset, paint)
            canvas.drawText(item.daQty.toString(), width * 0.65f, yOffset, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "₹${item.daTotalAmount.toInt()}",
                width - 20f,
                yOffset,
                paint
            )

            yOffset += 35f
        }


        yOffset += 20f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 26f
        paint.isFakeBoldText = true

        canvas.drawText(
            "$labelTotalAmount : ₹${totalAmount.toInt()}",
            width / 2f,
            yOffset,
            paint
        )

        paint.isFakeBoldText = false



        yOffset += 40f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        val boxTop = yOffset
        val boxBottom = boxTop + 200f

        canvas.drawRoundRect(
            20f,
            boxTop,
            width - 20f,
            boxBottom,
            20f,
            20f,
            paint
        )

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 22f

        canvas.drawText(labelVisitorDetails, width / 2f, boxTop + 35f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 20f

        canvas.drawText("$labelName : $name", 40f, boxTop + 75f, paint)
        canvas.drawText("$labelPhonenumber : $phoneNo", 40f, boxTop + 110f, paint)
        canvas.drawText("$labelIdNo : $idProof", 40f, boxTop + 145f, paint)

        yOffset = boxBottom + 40f


        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 24f
        canvas.drawText("Thank You!", width / 2f, yOffset, paint)

        yOffset += 30f
        paint.textSize = 16f
        canvas.drawText("Powered by www.xenionline.in", width / 2f, yOffset, paint)

        val finalBitmap =
            Bitmap.createBitmap(width, (yOffset + 40f).toInt(), Bitmap.Config.ARGB_8888)
        Canvas(finalBitmap).drawBitmap(tempBitmap, 0f, 0f, null)

        tempBitmap.recycle()
        return finalBitmap
    }


    private fun getLocalizedString(key: String, languageCode: String): String {
        return when (languageCode.lowercase()) {

            "en" -> key


            "ml" -> when (key) {
                "Receipt No" -> "രസീത് നമ്പർ"
                "Date" -> "തീയതി"
                "Item" -> "വസ്തു"
                "Name" -> "പേര്"
                "Phone No" -> "ഫോൺ നമ്പർ"
                "ID No" -> "ഐഡി നമ്പർ"
                "Price" -> "വില"
                "Devotees Details" -> "ഭക്തരുടെ വിശദാംശങ്ങൾ"
                "Qty" -> "അളവ്"
                "Amount" -> "തുക"
                "Total Amount" -> "ആകെ തുക"
                "UPI Reference No" -> "UPI Reference No"
                "There is NO Prasadam for Sheeghra Darshan" -> "ശീഘ്ര ദർശനത്തിന് പ്രസാദം ഇല്ല"
                else -> key
            }



            "hi" -> when (key) {
                "Receipt No" -> "रसीद संख्या"
                "Date" -> "तारीख"
                "Item" -> "वस्तु"
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
                "Item" -> "වස්තුව"
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
                "Item" -> "ವಸ್ತು"
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
                "Item" -> "பொருள்"
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
                "Item" -> "వస్తువు"
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
                "Item" -> "ਵਸਤੂ"
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
                "Item" -> "वस्तू"
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


    private fun generateQRCode(data: String): Bitmap? {
        val encryptedData = AESEncryptionUtil.encrypt(data, SECRET_KEY)
        val qrCodeWriter = QRCodeWriter()
        return try {
            val size = 300
            val bitMatrix: BitMatrix =
                qrCodeWriter.encode(encryptedData, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = createBitmap(width, height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bmp
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun redirect() {
        Handler(mainLooper).postDelayed({
            lifecycleScope.launch {
                ticketRepository.clearAllData()
            }
            val targetActivity = if (from == "billing") {
                LanguageActivity::class.java
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
