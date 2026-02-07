package com.xenia.ticket.utils.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.core.graphics.scale

class PineLabsPrinter(
    private val context: Context,
    private val serverMessenger: Messenger,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    fun printDailySummary(
        reportStart: String,
        reportEnd: String,
        order: Double,
        cash: Double,
        card: Double,
        upi: Double,
        net: Double,
        createdOn: String,
        generatedBy: String,
        selectedLanguage: String
    ) {
        scope.launch {
            try {
                val json = createDailySummaryPrintJson(
                    reportStart,
                    reportEnd,
                    order,
                    cash,
                    card,
                    upi,
                    net,
                    createdOn,
                    generatedBy,
                    selectedLanguage
                )

                val message = Message.obtain(null, PlutusConstants.MESSAGE_CODE)
                message.data = Bundle().apply {
                    putString(PlutusConstants.REQUEST_TAG, json)
                }
                message.replyTo = Messenger(IncomingHandler())

                Log.d("PINE_PRINT", "Sending print request")
                serverMessenger.send(message)

            } catch (e: Exception) {
                Log.e("PINE_PRINT", "Print error", e)
            }
        }
    }
    private suspend fun createDailySummaryPrintJson(
        reportStart: String,
        reportEnd: String,
        order: Double,
        cash: Double,
        card: Double,
        upi: Double,
        net: Double,
        createdOn: String,
        generatedBy: String,
        selectedLanguage: String
    ): String = withContext(Dispatchers.IO) {

        val headerBitmap = bitmapFileToHex("company_header.png", context.cacheDir) ?: ""
        val footerBitmap = bitmapFileToHex("company_footer.png", context.cacheDir) ?: ""

        val contentLines = generatePineLabDailySummery(
            reportTitle = "Daily Summary Report",
            reportStart = reportStart,
            reportEnd = reportEnd,
            order = order,
            cash = cash,
            card = card,
            upi = upi,
            net = net,
            createdOn = createdOn,
            generatedBy = generatedBy,
            selectedLanguage = selectedLanguage
        )

        val header = JSONObject().apply {
            put("ApplicationId", sessionManager.getPineLabsAppId())
            put("UserId", "admin")
            put("MethodId", PlutusConstants.METHOD_PRINT)
            put("VersionNo", "1.0")
        }

        val dataArray = JSONArray()

        if (headerBitmap.isNotEmpty()) {
            dataArray.put(imageLine(headerBitmap))
        }


        contentLines.forEach { line ->
            dataArray.put(textLine(line))
        }

        dataArray.put(smallSpaceLine)

        if (footerBitmap.isNotEmpty()) {
            dataArray.put(imageLine(footerBitmap))
        }
        dataArray.put(largeSpaceLine)
        val detail = JSONObject().apply {
            put("PrintRefNo", "PRN_${System.currentTimeMillis()}")
            put("SavePrintData", true)
            put("Data", dataArray)
        }

        JSONObject().apply {
            put("Header", header)
            put("Detail", detail)
        }.toString()
    }

    private fun textLine(text: String) = JSONObject().apply {
        put("PrintDataType", 0)
        put("PrinterWidth", 200)
        put("IsCenterAligned", false)
        put("DataToPrint", text)
        put("ImagePath", "")
        put("ImageData", "")
    }

    private fun imageLine(hex: String) = JSONObject().apply {
        put("PrintDataType", 2)
        put("PrinterWidth", 24)
        put("IsCenterAligned", true)
        put("DataToPrint", "")
        put("ImagePath", "")
        put("ImageData", hex)
    }
    val smallSpaceLine = JSONObject().apply {
        put("PrintDataType", 0)
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

    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val response = msg.data.getString(PlutusConstants.RESPONSE_TAG)
            Log.d("PLUTUS_RESPONSE", response ?: "NULL")

            if (response.isNullOrEmpty()) return

            try {
                val json = JSONObject(response)
                val status = json.optString("Status")
                if (status.equals("SUCCESS", true)) {
                    Log.d("PINE_PRINT", "Print SUCCESS")
                } else {
                    Log.e("PINE_PRINT", "Print FAILED: $response")
                }
            } catch (e: Exception) {
                Log.e("PINE_PRINT", "Invalid response", e)
            }
        }
    }
    suspend fun bitmapFileToHex(
        fileName: String,
        cacheDir: File,
        printerWidth: Int = 360
    ): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, fileName)
            if (!file.exists()) return@withContext null

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            val ratio = printerWidth.toFloat() / bitmap.width
            val resized = bitmap.scale(printerWidth, (bitmap.height * ratio).toInt())

            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.PNG, 100, out)

            out.toByteArray().joinToString("") { "%02X".format(it) }

        } catch (e: Exception) {
            null
        }
    }
    @SuppressLint("DefaultLocale")
    private fun generatePineLabDailySummery(
        reportTitle: String,
        reportStart: String,
        reportEnd: String,
        order: Double,
        cash: Double,
        card: Double,
        upi: Double,
        net: Double,
        createdOn: String,
        generatedBy: String,
        selectedLanguage: String
    ): List<String> {

        val lines = mutableListOf<String>()
        val WIDTH = 32
        val VALUE_WIDTH = 10

        fun divider() = "-".repeat(WIDTH)

        fun amountRow(label: String, value: Double) {
            lines.add(
                label.padEnd(WIDTH - VALUE_WIDTH) +
                        String.format("%.2f", value).padStart(VALUE_WIDTH)
            )
        }

        val title = when (selectedLanguage) {
            "ml" -> "സംഗ്രഹ റിപ്പോർട്ട്"
            "kn" -> "ಸಂಗ್ರಹ ವರದಿ"
            "ta" -> "சுருக்க அறிக்கை"
            "te" -> "సారాంశ నివేదిక"
            "hi" -> "सारांश रिपोर्ट"
            "pa" -> "ਸੰਖੇਪ ਰਿਪੋਰਟ"
            "mr" -> "सारांश अहवाल"
            "si" -> "සාරාංශ වාර්තාව"
            else -> "Summary Report"
        }

        // ===== TITLE =====
        lines.add(title.center(WIDTH))
        lines.add(divider())

        // ===== REPORT PERIOD =====
        lines.add("Report Period:")
        lines.add(reportStart)
        lines.add(reportEnd)
        lines.add(divider())

        // ===== AMOUNTS =====
        amountRow("Ticket", order)
        amountRow("Cash", cash)
        amountRow("Card", card)
        amountRow("UPI", upi)

        lines.add(divider())

        // ===== NET AMOUNT =====
        amountRow("Net Amount", net)

        lines.add(divider())

        // ===== FOOTER =====
        lines.add("Created : $createdOn")
        lines.add("Generated : $generatedBy")
        lines.add(divider())

        return lines
    }


    private fun String.center(width: Int): String {
        if (length >= width) return this
        val pad = (width - length) / 2
        return " ".repeat(pad) + this
    }

    private fun getLocalizedString(key: String, lang: String, includeBracket: Boolean): String {
        return when (lang) {
            "ml" -> mapOf(
                "Report Period" to "റിപ്പോർട്ട് കാലയളവ്",
                "Ticket" to "ടിക്കറ്റ്",
                "Net Amount" to "ആകെ തുക"
            )[key] ?: key
            else -> key
        }
    }
}
