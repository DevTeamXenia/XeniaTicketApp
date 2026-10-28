package com.xenia.ticket.utils.common

import android.annotation.SuppressLint
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.graphics.*
import android.os.IBinder
import android.os.Messenger
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.LifecycleCoroutineScope
import com.urovo.sdk.print.PrinterProviderImpl
import com.xenia.ticket.data.network.model.ItemSummaryReportResponse
import com.xenia.ticket.data.network.model.OfferItem
import com.xenia.ticket.data.repository.CompanySettingsRepository
import com.xenia.ticket.utils.pineLab.PineLabsPrinter
import com.xenia.ticket.utils.pineLab.PlutusConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.nyx.printerservice.print.IPrinterService
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import net.posprinter.POSConst
import net.posprinter.POSPrinter
import kotlinx.coroutines.delay
import android.hardware.usb.UsbManager
import com.xenia.ticket.utils.common.Constants.PRINTER_B200MAX
import com.xenia.ticket.utils.common.Constants.PRINTER_FALCON
import com.xenia.ticket.utils.common.Constants.PRINTER_KIOSK
import com.xenia.ticket.utils.common.Constants.PRINTER_PINELABS
import net.posprinter.utils.BitmapToByteData
import net.posprinter.utils.DataForSendToPrinterPos58
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

class ReportPrint(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val companyRepository: CompanySettingsRepository
) {

    private val appContext: Context = context.applicationContext
    private var lifecycleScope: LifecycleCoroutineScope? = null
    private var mPrintManager: PrinterProviderImpl? = null
    private var printerService: IPrinterService? = null
    private var curConnect: IDeviceConnection? = null
    private var isBound = false
    private var serverMessenger: Messenger? = null

    fun setLifecycleScope(scope: LifecycleCoroutineScope) {
        lifecycleScope = scope
        bindPineLabsService()

    }

    private fun bindPineLabsService() {
        val intent = Intent().apply {
            action = PlutusConstants.PLUTUS_ACTION
            setPackage(PlutusConstants.PLUTUS_PACKAGE)
        }
        appContext.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
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
    fun printDailySummary(
        reportStart: String,
        reportEnd: String,
        order: Double,
        cash: Double,
        card: Double,
        upi: Double,
        net: Double,
        generatedBy: String,
        selectedLanguage: String
    ) {
        lifecycleScope?.launch(Dispatchers.IO) {
            val selectedPrinter = sessionManager.getSelectedPrinter()
            var usbReady = false


            val currentDateTime = Calendar.getInstance().time
            val createdOnStr =
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH).format(currentDateTime)
            mPrintManager = PrinterProviderImpl.getInstance(appContext)

            if (selectedPrinter == "B200MAX") {
                bindB200MAXPrinter()
                delay(1000)
            } else if (selectedPrinter == "FALCON" || selectedPrinter == "KIOSK") {
                usbReady = connectUSBPrinter()
            }
            if (selectedPrinter == "PineLabs") {

                val messenger = serverMessenger
                if (messenger == null) {
                    Log.e("PINE_PRINT", "ServerMessenger is NULL. PineLabs service not connected.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "Printer not connected", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                PineLabsPrinter(
                    context = appContext,
                    serverMessenger = messenger,
                    sessionManager = sessionManager
                ).printDailySummary(
                    reportStart = reportStart,
                    reportEnd = reportEnd,
                    order = order,
                    cash = cash,
                    card = card,
                    upi = upi,
                    net = net,
                    createdOn = createdOnStr,
                    generatedBy = generatedBy,
                    selectedLanguage = selectedLanguage
                )

                return@launch
            }



            if ((selectedPrinter == "FALCON" || selectedPrinter == "KIOSK") && !usbReady) {
                Log.e("ReportPrint", "USB printer not ready — aborting print.")
                return@launch
            }


            val isHeaderEnabled = companyRepository.getBoolean(CompanyKey.COMPANYPRINT_H)
            val isFooterEnabled = companyRepository.getBoolean(CompanyKey.COMPANYPRINT_F)

            val headerFile: File? = companyRepository.getCompanyPrintHeaderFile()
            val footerFile: File? = companyRepository.getCompanyPrintFooterFile()

            val headerBitmap = if (
                isHeaderEnabled &&
                headerFile != null &&
                headerFile.exists() &&
                headerFile.length() > 0
            ) {
                BitmapFactory.decodeFile(headerFile.absolutePath)
            } else null

            val footerBitmap = if (
                isFooterEnabled &&
                footerFile != null &&
                footerFile.exists() &&
                footerFile.length() > 0
            ) {
                BitmapFactory.decodeFile(footerFile.absolutePath)
            } else null

            val summaryBitmap = generateDailySummaryBitmap(
                reportTitle = "Summary Report",
                reportStart = reportStart,
                reportEnd = reportEnd,
                order = order,
                cash = cash,
                card = card,
                upi = upi,
                net = net,
                createdOn = createdOnStr,
                generatedBy = generatedBy,
                selectedLanguage = selectedLanguage
            )

            try {
                if (selectedPrinter == "B200MAX") {

                    headerBitmap?.let { bmp ->
                        val scaled = bmp.scaleToWidth(550)
                        printerService?.printBitmap(scaled, 0, POSConst.ALIGNMENT_CENTER)
                        printerService?.printText("\n\n", null)
                        scaled.recycle()
                    }

                    printerService?.printBitmap(summaryBitmap, 0, POSConst.ALIGNMENT_CENTER)
                    printerService?.printText("\n\n", null)

                    footerBitmap?.let { bmp ->
                        val scaledFooter = bmp.scale(500, 100)


                        printerService?.printText("\n\n", null)


                        printerService?.printBitmap(scaledFooter, 0, POSConst.ALIGNMENT_CENTER)


                        printerService?.printText("\n\n\n", null)
                        scaledFooter.recycle()
                    } ?: run {

                        printerService?.printText("\n\n", null)
                    }


                    delay(1200)
                    printerService?.printEndAutoOut()

                } else {

                    curConnect?.let { conn ->
                        val printer = POSPrinter(conn)
                        printer.initializePrinter()
                        val targetWidth = getPrinterWidth()

                        headerBitmap?.let { bmp ->
                            printLargeBitmap(printer, bmp, targetWidth)
                            printer.feedLine(1)
                        }

                        printLargeBitmap(printer, summaryBitmap, targetWidth)
                        printer.feedLine(1)
                        delay(100)

                        try {
                            footerBitmap?.let { bmp ->
                                printLargeBitmap(printer, bmp, targetWidth)
                                printer.cutHalfAndFeed(1)
                            } ?: run {
                                printer.cutHalfAndFeed(1)
                            }
                        } catch (e: Exception) {
                            Log.e("ReportPrint", "Footer print error: ${e.message}")
                            printer.cutHalfAndFeed(1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportPrint", "Print Error: ${e.message}")
            } finally {
                summaryBitmap.recycle()
            }

        }
    }


    fun printItemSummaryReport(
        response: ItemSummaryReportResponse,
        fromDate: String,
        toDate: String,
        generatedBy: String,
        selectedLanguage: String
    ) {
        lifecycleScope?.launch(Dispatchers.IO) {
            val selectedPrinter = sessionManager.getSelectedPrinter()
            var usbReady = false

            val currentDateTime = Calendar.getInstance().time
            val createdOnStr =
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH).format(currentDateTime)
            mPrintManager = PrinterProviderImpl.getInstance(appContext)


            if (selectedPrinter == "PineLabs") {

            val messenger = serverMessenger
            if (messenger == null) {
                Log.e("PINE_PRINT", "ServerMessenger is NULL. PineLabs service not connected.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Printer not connected", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            PineLabsPrinter(
                context = appContext,
                serverMessenger = messenger,
                sessionManager = sessionManager
            ).printItemSummaryReport(
                response = response,
                fromDate = fromDate,
                toDate = toDate,
                generatedBy = generatedBy,
                createdOn = createdOnStr,
                selectedLanguage = selectedLanguage
            )

            return@launch
        }

            if (selectedPrinter == "B200MAX") {
                bindB200MAXPrinter()
                delay(1000)
            } else if (selectedPrinter == "FALCON" || selectedPrinter == "KIOSK") {
                usbReady = connectUSBPrinter()
            }

            if ((selectedPrinter == "FALCON" || selectedPrinter == "KIOSK") && !usbReady) {
                Log.e("ReportPrint", "USB printer not ready — aborting print.")
                return@launch
            }
            val isHeaderEnabled = companyRepository.getBoolean(CompanyKey.COMPANYPRINT_H)
            val isFooterEnabled = companyRepository.getBoolean(CompanyKey.COMPANYPRINT_F)

            val headerFile: File? = companyRepository.getCompanyPrintHeaderFile()
            val footerFile: File? = companyRepository.getCompanyPrintFooterFile()

            val headerBitmap = if (
                isHeaderEnabled &&
                headerFile != null &&
                headerFile.exists() &&
                headerFile.length() > 0
            ) {
                BitmapFactory.decodeFile(headerFile.absolutePath)
            } else null

            val footerBitmap = if (
                isFooterEnabled &&
                footerFile != null &&
                footerFile.exists() &&
                footerFile.length() > 0
            ) {
                BitmapFactory.decodeFile(footerFile.absolutePath)
            } else null
            val reportBitmap = generateDetailedReportBitmap(
                response = response,
                fromDate = fromDate,
                toDate = toDate,
                generatedBy = generatedBy,
                createdOn = createdOnStr,
                selectedLanguage = selectedLanguage
            )

            try {

                if (selectedPrinter == "B200MAX") {

                    headerBitmap?.let { bmp ->
                        val scaled = bmp.scaleToWidth(550)
                        printerService?.printBitmap(scaled, 0, POSConst.ALIGNMENT_CENTER)
                        printerService?.printText("\n\n", null)
                        scaled.recycle()
                    }

                    printerService?.printBitmap(reportBitmap, 0, POSConst.ALIGNMENT_CENTER)
                    printerService?.printText("\n\n", null)

                    footerBitmap?.let { bmp ->
                        val scaledFooter = bmp.scale(500, 120)
                        printerService?.printText("\n\n", null)
                        printerService?.printBitmap(scaledFooter, 0, POSConst.ALIGNMENT_CENTER)
                        printerService?.printText("\n\n", null)
                        scaledFooter.recycle()
                    } ?: run {
                        printerService?.printText("\n\n", null)
                    }

                    delay(1200)
                    printerService?.printEndAutoOut()

                } else {

                    curConnect?.let { conn ->
                        val printer = POSPrinter(conn)
                        printer.initializePrinter()
                        val targetWidth = getPrinterWidth()

                        headerBitmap?.let { bmp ->
                            printLargeBitmap(printer, bmp, targetWidth)
                            printer.feedLine(1)
                        }

                        printLargeBitmap(printer, reportBitmap, targetWidth)
                        printer.feedLine(1)

                        try {
                            footerBitmap?.let { bmp ->
                                printLargeBitmap(printer, bmp, targetWidth)
                                printer.cutHalfAndFeed(1)
                            } ?: run {
                                printer.cutHalfAndFeed(1)
                            }
                        } catch (e: Exception) {
                            Log.e("ReportPrint", "Footer print error: ${e.message}")
                            printer.cutHalfAndFeed(1)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("ReportPrint", "Error printing item summary: ${e.message}")
            } finally {

                reportBitmap.recycle()
                headerBitmap?.recycle()
                footerBitmap?.recycle()
            }
        }
    }

    private fun getPrinterWidth(): Int {
        val selectedPrinter = sessionManager.getSelectedPrinter()
        if (selectedPrinter == PRINTER_FALCON ||
            selectedPrinter == PRINTER_B200MAX ||
            selectedPrinter == PRINTER_PINELABS
        ) {
            return 384
        }

        try {
            val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
            val devices = usbManager?.deviceList?.values
            if (!devices.isNullOrEmpty()) {
                for (device in devices) {
                    if (device.vendorId == 0x0519 && device.productId == 0x2013) {
                        return 384
                    }
                    val devName = (device.productName.orEmpty() + " " + device.deviceName.orEmpty()).lowercase()
                    if (devName.contains("58") || devName.contains("ip1000") || devName.contains("ep58")) {
                        return 384
                    }
                    if (devName.contains("80") || devName.contains("ep80")) {
                        return 576
                    }
                }
            }
        } catch (_: Exception) {}

        val model = android.os.Build.MODEL.orEmpty()
        val manufacturer = android.os.Build.MANUFACTURER.orEmpty()
        if (model.contains("M2", ignoreCase = true) ||
            model.contains("Falcon", ignoreCase = true) ||
            manufacturer.contains("iMin", ignoreCase = true) ||
            manufacturer.contains("Urovo", ignoreCase = true)
        ) {
            return 384
        }

        return if (selectedPrinter == PRINTER_KIOSK) 576 else 384
    }

    private suspend fun printLargeBitmap(printer: POSPrinter, bitmap: Bitmap, targetWidth: Int = 576) {
        val scaledBitmap = if (bitmap.width != targetWidth) {
            val ratio = targetWidth.toFloat() / bitmap.width
            val targetHeight = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val sliceHeight = 120
        var startY = 0

        printer.sendData(DataForSendToPrinterPos58.selectAlignment(POSConst.ALIGNMENT_CENTER))

        while (startY < scaledBitmap.height) {
            val chunkHeight = minOf(sliceHeight, scaledBitmap.height - startY)
            val chunkBitmap = Bitmap.createBitmap(
                scaledBitmap,
                0,
                startY,
                scaledBitmap.width,
                chunkHeight
            )

            val rasterBytes = BitmapToByteData.rasterBmpToSendData(0, chunkBitmap)
            printer.sendData(rasterBytes)
            chunkBitmap.recycle()

            startY += chunkHeight
            delay(60)
        }

        printer.sendData(DataForSendToPrinterPos58.selectAlignment(0))

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
    }

    fun Bitmap.scaleToWidth(newWidth: Int): Bitmap {
        val ratio = newWidth.toFloat() / this.width
        val newHeight = (this.height * ratio).toInt()
        return this.scale(newWidth, newHeight)
    }

    private fun bindB200MAXPrinter() {
//        if (isBound) return
//        val intent = Intent().apply {
//            `package` = "com.incar.printerservice"
//            action = "com.incar.printerservice.IPrinterService"
//        }
//
//        serviceConnection = object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                printerService = IPrinterService.Stub.asInterface(service)
//                isBound = true
//                Log.d("ReportPrint", "B200MAX printer bound")
//            }
//
//            override fun onServiceDisconnected(name: ComponentName?) {
//                printerService = null
//                isBound = false
//            }
//        }
//
//        appContext.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    private suspend fun connectUSBPrinter(): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                POSConnect.init(appContext)
                val entries = POSConnect.getUsbDevices(appContext)

                if (entries.isEmpty()) {
                    Log.e("ReportPrint", "No USB printers found.")
                    cont.resume(false)
                    return@suspendCancellableCoroutine
                }

                curConnect?.close()
                curConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB)

                curConnect!!.connect(entries[0]) { code, _ ->
                    if (!cont.isActive) return@connect

                    when (code) {
                        POSConnect.CONNECT_SUCCESS -> {
                            Log.d("ReportPrint", "USB Printer connected successfully")
                            cont.resume(true)
                        }

                        POSConnect.CONNECT_FAIL,
                        POSConnect.CONNECT_INTERRUPT,
                        POSConnect.SEND_FAIL,
                        POSConnect.USB_DETACHED -> {
                            Log.e("ReportPrint", "USB Printer connection failed (code=$code)")
                            cont.resume(false)
                        }
                    }
                }

                cont.invokeOnCancellation {
                    try {
                        curConnect?.close()
                    } catch (e: Exception) {
                        Log.e("ReportPrint", "Error closing printer on cancel", e)
                    }
                }

            } catch (e: Exception) {
                Log.e("ReportPrint", "USB connection error: ${e.message}", e)
                if (cont.isActive) cont.resume(false)
            }
        }


    @SuppressLint("DefaultLocale")
    private fun generateDailySummaryBitmap(
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
        selectedLanguage: String,
        width: Int = 576
    ): Bitmap {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.MONOSPACE
        }

        val labelReportPeriod = getLocalizedString("Report Period", selectedLanguage, true)
        val labelDarshan = getLocalizedString("Ticket", selectedLanguage, true)
        val labelNetAmount = getLocalizedString("Net Amount", selectedLanguage, true)
        val labelCreated = getLocalizedString("Created", selectedLanguage, true)
        val labelGenerated = getLocalizedString("Generated", selectedLanguage, true)
        val englishTitle = "Summary Report"

        val localizedTitle = when (selectedLanguage.lowercase()) {
            "ml" -> "സംഗ്രഹ റിപ്പോർട്ട്"
            "kn" -> "ಸಾರಾಂಶ ವರದಿ"
            "ta" -> "சுருக்க அறிக்கை"
            "te" -> "సంక్షిప్త నివేదిక"
            "hi" -> "सारांश रिपोर्ट"
            "pa" -> "ਸੰਖੇਪ ਰਿਪੋਰਟ"
            "mr" -> "सारांश अहवाल"
            "si" -> "සාරාංශ වාර්තාව"
            else -> "Summary Report"
        }

        val bitmap = createBitmap(width, 1800)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = 50f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
        paint.isFakeBoldText = true
        y += 0f

        if (selectedLanguage.lowercase() == "ml") {
            canvas.drawText(localizedTitle, width / 2f, y, paint)
        } else if (selectedLanguage.lowercase() != "en") {
            canvas.drawText(localizedTitle, width / 2f, y, paint)
            paint.textSize = 26f
            y += 40f
            canvas.drawText(englishTitle, width / 2f, y, paint)
        } else {
            canvas.drawText(englishTitle, width / 2f, y, paint)
        }

        y += 40f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 22f
        canvas.drawText("----------------------------------------------------", 0f, y, paint)

        y += 35f
        canvas.drawText("$labelReportPeriod :", 20f, y, paint)
        y += 30f
        canvas.drawText(reportStart, 180f, y, paint)
        y += 30f
        canvas.drawText(reportEnd, 180f, y, paint)

        y += 20f
        canvas.drawText("----------------------------------------------------", 0f, y, paint)

        fun drawLineLocalized(label: String, englishLabel: String, value: Double) {
            val valueStr = String.format("%.2f", value)
            val maxLabelWidth = width - 120f

            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 18f
            var labelFontSize = 18f
            while (paint.measureText(label) > maxLabelWidth && labelFontSize > 14f) {
                labelFontSize -= 1f
                paint.textSize = labelFontSize
            }

            y += 25f

            when (selectedLanguage.lowercase()) {
                "en" -> {
                    canvas.drawText(englishLabel, 15f, y, paint)
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(valueStr, width - 20f, y, paint)
                }

                "ml" -> {

                    canvas.drawText(label, 15f, y, paint)
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(valueStr, width - 20f, y, paint)
                }

                else -> {

                    canvas.drawText(label, 15f, y, paint)
                    y += 22f
                    paint.textSize = 16f
                    canvas.drawText(englishLabel, 20f, y, paint)
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(valueStr, width - 20f, y, paint)
                }
            }
        }
        drawLineLocalized(labelDarshan, "Ticket", order)

        y += 25f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("----------------------------------------------------", 0f, y, paint)


        fun drawLine(label: String, value: Double) {
            y += 30f
            val valueStr = String.format("%.2f", value)
            val maxLabelWidth = width - 120f
            var labelFontSize = 22f

            paint.textSize = labelFontSize
            while (paint.measureText(label) > maxLabelWidth && labelFontSize > 16f) {
                labelFontSize -= 1f
                paint.textSize = labelFontSize
            }

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(valueStr, width - 20f, y, paint)
            paint.textSize = 22f
        }

        drawLine("Cash", cash)
        drawLine("Card", card)
        drawLine("UPI", upi)

        y += 25f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("----------------------------------------------------", 0f, y, paint)


        y += 40f
        var netLabelFontSize = 22f
        paint.textSize = netLabelFontSize
        while (paint.measureText(labelNetAmount) > width - 120f && netLabelFontSize > 14f) {
            netLabelFontSize -= 1f
            paint.textSize = netLabelFontSize
        }

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(labelNetAmount, 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        val netText = String.format("%.2f", net)
        canvas.drawText(netText, width - 10f, y, paint)

        y += 30f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 24f
        canvas.drawText("----------------------------------------------------", 0f, y, paint)


        y += 50f
        paint.textSize = 22f
        canvas.drawText("$labelCreated : $createdOn", 20f, y, paint)
        y += 30f
        canvas.drawText("$labelGenerated : $generatedBy", 20f, y, paint)


        val final = createBitmap(width, y.toInt() + 40)
        val finalCanvas = Canvas(final)
        finalCanvas.drawColor(Color.WHITE)
        finalCanvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return final
    }



    @SuppressLint("DefaultLocale")
    fun generateItemSummaryBitmap2inch(
        response: ItemSummaryReportResponse,
        fromDate: String,
        toDate: String,
        generatedBy: String,
        createdOn: String,
        selectedLanguage: String
    ): Bitmap {

        val width = 384
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.MONOSPACE
        }

        val bitmap = createBitmap(width, 2000)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = 40f

        fun draw(text: String, size: Float = 20f, bold: Boolean = false, center: Boolean = false) {
            paint.textSize = size
            paint.isFakeBoldText = bold
            paint.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            canvas.drawText(text, if (center) width / 2f else 15f, y, paint)
            y += size + 8f
        }


        val englishTitle = "Item Summary Report"
        val localizedTitle = when (selectedLanguage.lowercase()) {
            "ml" -> "സംഗ്രഹ റിപ്പോർട്ട്"
            "kn" -> "ಸಂಗ್ರಹ ವರದಿ"
            "ta" -> "சுருக்க அறிக்கை"
            "te" -> "సారాంశ నివేదిక"
            "hi" -> "सारांश रिपोर्ट"
            "pa" -> "ਸੰਖੇਪ ਰਿਪੋਰਟ"
            "mr" -> "सारांश अहवाल"
            "si" -> "සාරාංශ වාර්තාව"
            else -> englishTitle
        }

        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        paint.textSize = 26f
        canvas.drawText(localizedTitle, width / 2f, y, paint)

        if (selectedLanguage.lowercase() != "en") {
            y += 30f
            paint.textSize = 20f
            canvas.drawText(englishTitle, width / 2f, y, paint)
        }

        y += 35f
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = false

        draw("------------------------------------")
        draw("From : $fromDate")
        draw("To   : $toDate")
        draw("Generated By : $generatedBy")
        draw("Created On   : $createdOn")
        draw("------------------------------------")


        fun printSection(title: String, list: List<OfferItem>, total: Double) {
            draw(title.uppercase(), 22f, true)
            draw("------------------------------------")
            draw("Ticket              Qty   Total", 18f, true)

            list.forEach {
                val name = (it.offerName ?: it.ticketName ?: "").take(15)
                val qty = it.totalQty.toString()
                val amt = String.format("%.2f", it.totalAmount)


                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(name, 15f, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(qty, width - 80f, y, paint)
                canvas.drawText(amt, width - 15f, y, paint)
                y += 22f
            }

            draw("------------------------------------")
            draw("Section Total : ${String.format("%.2f", total)}", 20f, true)
            draw("------------------------------------")
        }

        if (response.darshanTickets.isNotEmpty()) {
            printSection("TICKETS", response.darshanTickets, response.summary.darshanTickets.GrandTotalAmount)
        }


        draw("GRAND TOTAL : ${String.format("%.2f", response.summary.GrandTotalAmountAll)}", 24f, true)
        draw("------------------------------------")


        val final = createBitmap(width, y.toInt() + 30)
        val finalCanvas = Canvas(final)
        finalCanvas.drawColor(Color.WHITE)
        finalCanvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return final
    }

    @SuppressLint("DefaultLocale")
    fun generateDetailedReportBitmap(
        response: ItemSummaryReportResponse,
        fromDate: String,
        toDate: String,
        generatedBy: String,
        createdOn: String,
        selectedLanguage: String,
        width: Int = 576
    ): Bitmap {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.MONOSPACE
        }


        val englishTitle = "Detailed Report"
        val localizedTitle = when (selectedLanguage.lowercase()) {
            "ml" -> "Detailed Report"
            "ta" -> "Detailed Report"
            "te" -> "Detailed Report"
            "hi" -> "Detailed Report"
            "kn" -> "Detailed Report"
            "si"-> "Detailed Report"
            "pa" -> "Detailed Report"
            "mr" -> "Detailed Report"
            else -> englishTitle
        }

        val bitmap = createBitmap(width, 3000)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = 60f


        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
        paint.isFakeBoldText = true
        if (selectedLanguage.lowercase() == "en") {
            canvas.drawText(englishTitle, width / 2f, y, paint)
        } else {
            canvas.drawText(localizedTitle, width / 2f, y, paint)
            y += 38f
            paint.textSize = 26f
            canvas.drawText(englishTitle, width / 2f, y, paint)
        }


        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 22f
        y += 50f
        canvas.drawText("----------------------------------------------------", 0f, y, paint)
        y += 35f
        canvas.drawText("From : $fromDate", 20f, y, paint)
        y += 30f
        canvas.drawText("To   : $toDate", 20f, y, paint)
        y += 30f
        canvas.drawText("Generated By : $generatedBy", 20f, y, paint)
        y += 30f
        canvas.drawText("Created On   : $createdOn", 20f, y, paint)
        y += 30f
        canvas.drawText("----------------------------------------------------", 0f, y, paint)


        fun printSection(titleEn: String, localizedTitle: String, items: List<OfferItem>, total: Double) {
            if (items.isEmpty()) return

            y += 40f
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 28f
            paint.isFakeBoldText = true
            if (selectedLanguage.lowercase() == "en") {
                canvas.drawText(titleEn, width / 2f, y, paint)
            } else {
                canvas.drawText(localizedTitle, width / 2f, y, paint)
                y += 35f
                paint.textSize = 24f
                paint.isFakeBoldText = false
                canvas.drawText(titleEn, width / 2f, y, paint)
            }

            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 22f
            paint.isFakeBoldText = true
            y += 35f
            canvas.drawText("----------------------------------------------------", 0f, y, paint)
            y += 30f
            canvas.drawText("Ticket                  Qty      Amount", 20f, y, paint)
            y += 20f
            paint.isFakeBoldText = false

            for (item in items) {
                val name = (item.offerName ?: item.ticketName ?: "").take(18)
                val qty = item.totalQty.toString()
                val amount = String.format("%.2f", item.totalAmount)

                y += 30f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(name, 20f, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(qty, width - 160f, y, paint)
                canvas.drawText(amount, width - 20f, y, paint)
            }

            y += 35f
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("Section Total :", 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format("%.2f", total), width - 20f, y, paint)

            y += 30f
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 22f
            paint.isFakeBoldText = false
            canvas.drawText("----------------------------------------------------", 0f, y, paint)
        }


        printSection(
            " Tickets",
            when (selectedLanguage.lowercase()) {
                "ml" -> " Tickets"
                "ta" -> " Tickets"
                "te" -> " Tickets"
                "hi" -> " Tickets"
                "kn" -> " Tickets"
                "si" -> " Tickets"
                "mr" -> " Tickets"
                "pa" -> " Tickets"
                else -> " Tickets"
            },
            response.darshanTickets,
            response.summary.darshanTickets.GrandTotalAmount
        )

        y += 45f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("GRAND TOTAL :", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            String.format("%.2f", response.summary.GrandTotalAmountAll),
            width - 20f,
            y,
            paint
        )

        y += 50f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 22f
        paint.isFakeBoldText = false
        canvas.drawText("----------------------------------------------------", 0f, y, paint)

        val finalBitmap = createBitmap(width, y.toInt() + 50)
        val finalCanvas = Canvas(finalBitmap)
        finalCanvas.drawColor(Color.WHITE)
        finalCanvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        return finalBitmap
    }


    private fun getLocalizedString(
        key: String,
        language: String,
        includeBracketText: Boolean
    ): String {
        return when (language) {

            "ml" -> when (key) {
                "Report Period" -> "റിപ്പോർട്ട് കാലയളവ്"
                "Ticket" -> "ടിക്കറ്റ്"
                "Net Amount" -> "ആകെ തുക"
                "Created" -> "Created"
                "Generated" -> "Generated"
                else -> key
            }

            "si" -> when (key) {
                "Report Period" -> "වාර්තාවේ කාලය"
                "Ticket" -> "ටිකට්පත"
                "Net Amount" -> if (includeBracketText)
                    "මුළු මුදල (Net Amount)"
                else
                    "මුළු මුදල"
                "Created" -> "නිර්මාණය කරන ලදි"
                "Generated" -> "සාදන ලදි"
                else -> key
            }

            "hi" -> when (key) {
                "Report Period" -> "रिपोर्ट अवधि"
                "Ticket" -> "टिकट"
                "Net Amount" -> if (includeBracketText)
                    "कुल राशि (Net Amount)"
                else
                    "कुल राशि"
                "Created" -> "Created"
                "Generated" -> "Generated"
                else -> key
            }

            "ta" -> when (key) {
                "Report Period" -> "அறிக்கை காலம்"
                "Ticket" -> "டிக்கெட்"
                "Net Amount" -> if (includeBracketText)
                    "மொத்த தொகை (Net Amount)"
                else
                    "மொத்த தொகை"
                "Created" -> "Created"
                "Generated" -> "Generated"
                else -> key
            }

            "kn" -> when (key) {
                "Report Period" -> "ವರದಿ ಅವಧಿ"
                "Ticket" -> "ಟಿಕೆಟ್"
                "Net Amount" -> if (includeBracketText)
                    "ಒಟ್ಟು ಮೊತ್ತ (Net Amount)"
                else
                    "ಒಟ್ಟು ಮೊತ್ತ"
                "Created" -> "Created"
                "Generated" -> "Generated"
                else -> key
            }

            "te" -> when (key) {
                "Report Period" -> "రిపోర్ట్ కాలం"
                "Ticket" -> "టికెట్"
                "Net Amount" -> if (includeBracketText)
                    "మొత్తం మొత్తం (Net Amount)"
                else
                    "మొత్తం మొత్తం"
                "Created" -> "Created"
                "Generated" -> "Generated"
                else -> key
            }

            "pa" -> when (key) {
                "Report Period" -> "ਰਿਪੋਰਟ ਅਵਧੀ"
                "Ticket" -> "ਟਿਕਟ"
                "Net Amount" -> if (includeBracketText)
                    "ਕੁੱਲ ਰਕਮ (Net Amount)"
                else
                    "ਕੁੱਲ ਰਕਮ"
                "Created" -> "Created"
                "Generated" -> "Generated"
                else -> key
            }

            "mr" -> when (key) {
                "Report Period" -> "अहवाल कालावधी"
                "Ticket" -> "तिकीट"
                "Net Amount" -> if (includeBracketText)
                    "एकूण रक्कम (Net Amount)"
                else
                    "एकूण रक्कम"
                "Created" -> "Created"
                "Generated" -> "Generated"
                else -> key
            }

            else -> key
        }
    }




}