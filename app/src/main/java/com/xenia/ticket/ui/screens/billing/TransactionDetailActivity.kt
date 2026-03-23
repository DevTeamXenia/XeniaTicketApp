package com.xenia.ticket.ui.screens.billing

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.*
import android.util.Log
import android.view.Window
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.data.network.model.TransactionItem
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.ReportRepository
import com.xenia.ticket.databinding.ActivityTransactionDetailBinding
import com.xenia.ticket.ui.adapter.TransactionDetailAdapter
import com.xenia.ticket.utils.pineLab.PlutusConstants
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.koin.android.ext.android.inject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnReprint: Button
    private lateinit var progressBar: ProgressBar

    private val repository: ReportRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private val sessionManager: SessionManager by inject()
    private var orderId: Int = 0
    private var transactionId: String? = null
    private var receiptNumber: String? = null
    private var name: String? = null
    private var phoneNo: String? = null


    // PineLabs
    private var serverMessenger: Messenger? = null
    private var isBound = false

    // ---------------- Activity Lifecycle ----------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            RecyclerView.LayoutParams.WRAP_CONTENT
        )

        orderId = intent.getIntExtra("orderId", 0)
        transactionId = intent.getStringExtra("transID")
        receiptNumber = intent.getStringExtra("receiptNumber")
        name = intent.getStringExtra("name")
        phoneNo = intent.getStringExtra("phoneNo")

        recyclerView = binding.recyclerView
        btnReprint = binding.btnReprint
        progressBar = binding.progressBar

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = TransactionDetailAdapter()
        recyclerView.adapter = adapter

        bindPineLabsService()
        fetchTransactionDetails(adapter)

        btnReprint.setOnClickListener {
            if (!isBound) {
                Toast.makeText(this, "Printer not ready", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Handler(Looper.getMainLooper()).postDelayed({
                sendPrintRequest()
            }, 500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(serviceConnection)
    }

    // ---------------- Load Transaction Details ----------------

    private fun fetchTransactionDetails(adapter: TransactionDetailAdapter) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val response = repository.fetchTransactionDetails(orderId)
                adapter.submitList(response)
            } catch (e: Exception) {
                Toast.makeText(
                    this@TransactionDetailActivity,
                    "Failed to load transaction",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    // ---------------- PineLabs Service Binding ----------------

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
            Log.d("PLUTUS", "Service Connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverMessenger = null
            isBound = false
            Log.d("PLUTUS", "Service Disconnected")
        }
    }

    // ---------------- Send Print Request ----------------

    private fun sendPrintRequest() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                btnReprint.isEnabled = false

                val printJson = buildPlutusPrintJson()

                val message = Message.obtain(null, PlutusConstants.MESSAGE_CODE)
                val bundle = Bundle()

                bundle.putString(PlutusConstants.REQUEST_TAG, printJson)
                message.data = bundle
                message.replyTo = Messenger(IncomingHandler())

                serverMessenger?.send(message)

            } catch (e: Exception) {
                Toast.makeText(
                    this@TransactionDetailActivity,
                    e.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------- Handle PineLabs Response ----------------

    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val response = msg.data.getString(PlutusConstants.RESPONSE_TAG)
            Log.d("PLUTUS_RESPONSE", response ?: "NULL")

            progressBar.visibility = View.GONE
            btnReprint.isEnabled = true

            if (response.isNullOrEmpty()) {
                Toast.makeText(
                    this@TransactionDetailActivity,
                    "No response from printer",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val json = JSONObject(response)
            if (json.optString("Status").equals("SUCCESS", true)) {
                Toast.makeText(
                    this@TransactionDetailActivity,
                    "Receipt Printed Successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this@TransactionDetailActivity,
                    "Print Failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------- Build PineLabs Print JSON ----------------

    private suspend fun buildPlutusPrintJson(): String {
        val details: List<TransactionItem> =
            repository.fetchTransactionDetails(orderId)

        val currentDate = SimpleDateFormat(
            "dd-MMM-yyyy hh:mm a",
            Locale.ENGLISH
        ).format(Date())
        val headerBitmap = bitmapFileToHex("company_header.png", cacheDir) ?: ""
        val footerBitmap = bitmapFileToHex("company_footer.png", cacheDir) ?: ""
        val receiptLines =
            if (companyRepository.getDefaultLanguage() == sessionManager.getBillingSelectedLanguage()) {
                generateReceiptTextDefault(
                    currentDate,
                    transactionId,
                    receiptNumber.toString(),
                    details,
                    sessionManager.getBillingSelectedLanguage()
                )
            } else {
                generateReceiptText(
                    currentDate,
                    transactionId,
                    receiptNumber.toString(),
                    details,
                    sessionManager.getBillingSelectedLanguage()
                )
            }

        val header = JSONObject().apply {
            put("ApplicationId", sessionManager.getPineLabsAppId())
            put("UserId", "admin")
            put("MethodId", PlutusConstants.METHOD_PRINT)
            put("VersionNo", "1.0")
        }

        val headerImageLine = JSONObject().apply {
            put("PrintDataType", 2)
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", "")
            put("ImagePath", "")
            put("ImageData", headerBitmap)
        }

        val footerImageLine = JSONObject().apply {
            put("PrintDataType", 2)
            put("PrinterWidth", 24)
            put("IsCenterAligned", true)
            put("DataToPrint", "")
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
            receiptLines.forEach { line ->

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
                            put("DataToPrint", "")
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
            }
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

        val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val textWidth = paint.measureText(text)
        val x = (bitmapWidth - textWidth) / 2f   // 🔥 CENTERING
        val y = -fm.top + 10

        canvas.drawText(text, x, y, paint)

        return bitmapToHex(bitmap)
    }

    private fun bitmapToHex(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return byteArray.joinToString(separator = "") {
            String.format("%02X", it)
        }
    }
    private suspend fun generateReceiptText(
        currentDate: String,
        transID: String?,
        orderID: String?,
        ticket: List<TransactionItem>,
        selectedLanguage: String
    ): List<String> {

        val lines = mutableListOf<String>()
        val LINE_WIDTH = 45
        val ITEM_WIDTH = 16
        val PRICE_WIDTH = 6
        val QTY_WIDTH = 4
        val AMT_WIDTH = 8
        val totalWidth = ITEM_WIDTH + PRICE_WIDTH + QTY_WIDTH + AMT_WIDTH + 3  // ~37, for dashes
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
            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.Rate)
            val qtyStr = item.Quantity.toString()
            val amtStr = String.format(Locale.ENGLISH, "%.2f", item.Rate)
            totalAmount += item.Rate

            val itemName = when (selectedLanguage.lowercase()) {
                "ml" -> item.TicketNameMa
                "hi" -> item.TicketNameHi
                "ta" -> item.TicketNameTa
                "kn" -> item.TicketNameKa
                "te" -> item.TicketNameTe
                "si" -> item.TicketNameSi
                "pa" -> item.TicketNamePa
                "mr" -> item.TicketNameMr
                else -> item.TicketName
            }

            val itemDName = when (defaultLang.lowercase()) {
                "ml" -> item.TicketNameMa
                "hi" -> item.TicketNameHi
                "ta" -> item.TicketNameTa
                "kn" -> item.TicketNameKa
                "te" -> item.TicketNameTe
                "si" -> item.TicketNameSi
                "pa" -> item.TicketNamePa
                "mr" -> item.TicketNameMr
                else -> item.TicketName
            }

            lines.add(" ")

            itemName.chunked(totalWidth + 10).forEach { lines.add(it) }
            itemDName.chunked(totalWidth + 10).forEach { lines.add(it) }

            lines.add("")

            lines.add(
                "".padRight(ITEM_WIDTH) +
                        priceStr.padLeft(PRICE_WIDTH) + " " +
                        qtyStr.padLeft(QTY_WIDTH) + " " +
                        amtStr.padLeft(AMT_WIDTH)
            )
        }
        lines.add("-".repeat(totalWidth))
        lines.add(
            labelTotalAmount.padRight(totalWidth - AMT_WIDTH) +
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
        ticket: List<TransactionItem>,
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
            totalAmount += item.Rate

            val itemName = when (selectedLanguage.lowercase()) {
                "ml" -> item.TicketNameMa
                "hi" -> item.TicketNameHi
                "ta" -> item.TicketNameTa
                "kn" -> item.TicketNameKa
                "te" -> item.TicketNameTe
                "si" -> item.TicketNameSi
                "pa" -> item.TicketNamePa
                "mr" -> item.TicketNameMr
                else -> item.TicketName
            }

            val priceStr = String.format(Locale.ENGLISH, "%.2f", item.Rate)
            val qtyStr = item.Quantity.toString()
            val amountStr = String.format(Locale.ENGLISH, "%.2f", item.Rate)
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
                val resizedBitmap =
                    Bitmap.createScaledBitmap(originalBitmap, printerWidth, newHeight, true)

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
}