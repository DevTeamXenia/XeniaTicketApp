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
import com.xenia.ticket.data.network.model.ItemSummaryReportResponse

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
            put("ApplicationId", "d585cf57dc5f4dab9e99fc1d37fa1333")
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
        lines.add(title.center(WIDTH))
        lines.add(divider())
        lines.add("Report Period:")

        lines.add(reportStart)
        lines.add(reportEnd)
        lines.add(divider())

        amountRow("Ticket", order)
        amountRow("Cash", cash)
        amountRow("Card", card)
        amountRow("UPI", upi)

        lines.add(divider())
        amountRow("Net Amount", net)
        lines.add(divider())

        lines.add("Created : $createdOn")
        lines.add("Generated : $generatedBy")
        lines.add(divider())

        return lines
    }


    fun printItemSummaryReport(
        response: ItemSummaryReportResponse,
        fromDate: String,
        toDate: String,
        generatedBy: String,
        createdOn: String,
        selectedLanguage: String
    ) {
        scope.launch {
            try {
                val json = createItemSummaryPrintJson(
                    response,
                    fromDate,
                    toDate,
                    generatedBy,
                    createdOn,
                    selectedLanguage
                )

                val message = Message.obtain(null, PlutusConstants.MESSAGE_CODE)
                message.data = Bundle().apply {
                    putString(PlutusConstants.REQUEST_TAG, json)
                }
                message.replyTo = Messenger(IncomingHandler())

                Log.d("PINE_PRINT", "Sending Item Summary print request")
                serverMessenger.send(message)

            } catch (e: Exception) {
                Log.e("PINE_PRINT", "Item Summary print error", e)
            }
        }
    }
    private suspend fun createItemSummaryPrintJson(
        response: ItemSummaryReportResponse,
        fromDate: String,
        toDate: String,
        generatedBy: String,
        createdOn: String,
        selectedLanguage: String
    ): String = withContext(Dispatchers.IO) {

        val headerBitmap = bitmapFileToHex("company_header.png", context.cacheDir) ?: ""
        val footerBitmap = bitmapFileToHex("company_footer.png", context.cacheDir) ?: ""

        val lines = generatePineLabsItemSummaryLines(
            response,
            fromDate,
            toDate,
            generatedBy,
            createdOn,
            selectedLanguage
        )

        val header = JSONObject().apply {
            put("ApplicationId", "d585cf57dc5f4dab9e99fc1d37fa1333")
            put("UserId", "admin")
            put("MethodId", PlutusConstants.METHOD_PRINT)
            put("VersionNo", "1.0")
        }

        val dataArray = JSONArray()

        if (headerBitmap.isNotEmpty()) {
            dataArray.put(imageLine(headerBitmap))
        }

        lines.forEach { line ->
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

    @SuppressLint("DefaultLocale")
    private fun generatePineLabsItemSummaryLines(
        response: ItemSummaryReportResponse,
        fromDate: String,
        toDate: String,
        generatedBy: String,
        createdOn: String,
        selectedLanguage: String
    ): List<String> {

        val lines = mutableListOf<String>()

        val WIDTH = 30
        val QTY_WIDTH = 4
        val AMT_WIDTH = 8
        val NAME_WIDTH = WIDTH - QTY_WIDTH - AMT_WIDTH


        fun divider() = "-".repeat(WIDTH)

        fun center(text: String): String {
            if (text.length >= WIDTH) return text
            val padding = (WIDTH - text.length) / 4
            return " ".repeat(padding) + text
        }

        fun wrapText(text: String, width: Int): List<String> {
            val result = mutableListOf<String>()
            var remaining = text.trim()

            while (remaining.length > width) {
                var splitIndex = remaining.lastIndexOf(' ', width)
                if (splitIndex == -1) splitIndex = width
                result.add(remaining.substring(0, splitIndex))
                remaining = remaining.substring(splitIndex).trim()
            }

            if (remaining.isNotEmpty()) {
                result.add(remaining)
            }

            return result
        }

        fun row(name: String, qty: Double, amt: Double) {

            val wrappedName = wrapText(name, NAME_WIDTH)

            if (wrappedName.size == 1) {
                lines.add(
                    wrappedName[0].padEnd(NAME_WIDTH) +
                            String.format("%.0f", qty).padStart(QTY_WIDTH) +
                            String.format("%.2f", amt).padStart(AMT_WIDTH)
                )
            } else {

                // Print all lines except last (name only)
                for (i in 0 until wrappedName.size - 1) {
                    lines.add(wrappedName[i])
                }

                // Last line with qty & amount
                lines.add(
                    wrappedName.last().padEnd(NAME_WIDTH) +
                            String.format("%.0f", qty).padStart(QTY_WIDTH) +
                            String.format("%.2f", amt).padStart(AMT_WIDTH)
                )
            }
        }


        val title = "Detailed Report"

        lines.add(center(title))
        lines.add(divider())
        lines.add("From : $fromDate")
        lines.add("To   : $toDate")
        lines.add("By   : $generatedBy")
        lines.add("On   : $createdOn")
        lines.add(divider())


        if (response.darshanTickets.isNotEmpty()) {

            lines.add(center("Tickets"))
            lines.add(divider())

            lines.add(
                "Item".padEnd(NAME_WIDTH) +
                        "Qty".padStart(QTY_WIDTH) +
                        "Amt".padStart(AMT_WIDTH)
            )

            lines.add(divider())

            response.darshanTickets.forEach {
                row(
                    it.offerName ?: it.ticketName ?: "",
                    it.totalQty,
                    it.totalAmount
                )
            }

            lines.add(divider())

            lines.add(
                "Section Total".padEnd(WIDTH - AMT_WIDTH) +
                        String.format("%.2f",
                            response.summary.darshanTickets.GrandTotalAmount
                        ).padStart(AMT_WIDTH)
            )

            lines.add(divider())
        }


        lines.add(
            "GRAND TOTAL".padEnd(WIDTH - AMT_WIDTH) +
                    String.format("%.2f",
                        response.summary.GrandTotalAmountAll
                    ).padStart(AMT_WIDTH)
        )

        lines.add(divider())

        return lines
    }
    
    private fun String.center(width: Int): String {
        if (length >= width) return this
        val pad = (width - length) / 2
        return " ".repeat(pad) + this
    }
}
