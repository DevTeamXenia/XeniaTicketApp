package com.xenia.ticket.utils.common

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.padEnd
import kotlin.text.take
import kotlin.text.toByteArray

object AESEncryptionUtil {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_LENGTH = 32
    private val IV = ByteArray(16)


    private fun fixKeyLength(key: String): String {
        return when {
            key.length > KEY_LENGTH -> key.take(KEY_LENGTH)
            key.length < KEY_LENGTH -> key.padEnd(KEY_LENGTH, '0')
            else -> key
        }
    }

    fun encrypt(data: String, key: String): String {
        val fixedKey = fixKeyLength(key).toByteArray(StandardCharsets.UTF_8)
        val secretKey = SecretKeySpec(fixedKey, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivParameterSpec = IvParameterSpec(IV)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        val encryptedBytes = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }
}
