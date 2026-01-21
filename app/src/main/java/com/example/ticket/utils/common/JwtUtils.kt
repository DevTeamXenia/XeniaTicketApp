package com.example.ticket.utils.common



import android.util.Base64
import org.json.JSONObject
import kotlin.text.split
import kotlin.text.toInt

object JwtUtils {

    fun decodeJwt(token: String): JSONObject {
        val payload = token.split(".")[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
        return JSONObject(String(decodedBytes))
    }
    fun getUserId(token: String): Int? = try {
        decodeJwt(token).getString("UserId").toInt()
    } catch (e: Exception) {
        null
    }
    fun getUsername(token: String): String? = try {
        decodeJwt(token).getString("sub")
    } catch (e: Exception) {
        null
    }
    fun getUserType(token: String): String? = try {
        val json = decodeJwt(token)
        json.optString(
            "UserType",
            json.optString(
                "http://schemas.microsoft.com/ws/2008/06/identity/claims/role"
            )
        )
    } catch (e: Exception) {
        null
    }



}
