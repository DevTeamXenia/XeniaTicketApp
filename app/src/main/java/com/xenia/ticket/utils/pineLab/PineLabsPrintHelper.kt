package com.xenia.ticket.utils.pineLab

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import com.xenia.ticket.data.room.entity.Orders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object  PineLabsPrintHelper {

    fun bitmapToHex(bitmap: Bitmap): String {

        val resized = bitmap.scale(360, bitmap.height)

        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val bytes = stream.toByteArray()

        return bytes.joinToString("") { "%02X".format(it) }
    }

    suspend fun bitmapFileToHex(fileName: String, cacheDir: File, printerWidth: Int = 360, quality: Int = 90): String? {
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

    fun breakTextIntoLines(
        text: String,
        maxWidth: Float,
        paint: Paint
    ): List<String> {

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {

            val testLine =
                if (currentLine.isEmpty()) word else "$currentLine $word"

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) lines.add(currentLine)

        return lines
    }

    fun calculateReceiptHeight(
        ticket: List<Orders>,
        paint: Paint,
        width: Int
    ): Int {

        var y = 60f

        y += 40f
        y += 60f
        y += 60f
        y += 60f
        y += 50f

        for (item in ticket) {

            val itemName = item.ticketName ?: ""

            val lines = breakTextIntoLines(itemName, width * 0.9f, paint)

            y += (lines.size * 35f)

            y += 60f
        }

        y += 200f
        y += 250f

        return y.toInt()
    }

     fun bitmapFileToBitmap(fileName: String, cacheDir: File): Bitmap? {
        return try {
            val file = File(cacheDir, fileName)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    fun combineBitmaps(
        header: Bitmap?,
        body: Bitmap,
        footer: Bitmap?
    ): Bitmap {

        val width = 384

        val headerScaled = header?.let {
            Bitmap.createScaledBitmap(it, width, it.height, true)
        }

        val footerScaled = footer?.let {
            Bitmap.createScaledBitmap(it, width, it.height, true)
        }

        val totalHeight =
            (headerScaled?.height ?: 0) +
                    body.height +
                    (footerScaled?.height ?: 0)

        val result = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)

        var y = 0f

        headerScaled?.let {
            canvas.drawBitmap(it, 0f, y, null)
            y += it.height
        }

        canvas.drawBitmap(body, 0f, y, null)
        y += body.height

        footerScaled?.let {
            canvas.drawBitmap(it, 0f, y, null)
        }

        return result
    }


}