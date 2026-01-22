package com.example.ticket.utils.common

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    fun decodeJwt(token: String): JSONObject {
        val payload = token.split(".")[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
        return JSONObject(String(decodedBytes))
    }

    /** sub = user id */
    fun getUserId(token: String): Int? = try {
        decodeJwt(token).getString("sub").toInt()
    } catch (e: Exception) {
        null
    }

    /** unique_name = username */
    fun getUsername(token: String): String? = try {
        decodeJwt(token).getString("unique_name")
    } catch (e: Exception) {
        null
    }

    /** userType = CounterUser | User | ProcessUser */
    fun getUserType(token: String): String? = try {
        decodeJwt(token).getString("userType")
    } catch (e: Exception) {
        null
    }

    fun getCompanyId(token: String): Int? = try {
        decodeJwt(token).getString("companyId").toInt()
    } catch (e: Exception) {
        null
    }

    fun getSubscriptionStatus(token: String): String? = try {
        decodeJwt(token).getString("subscriptionStatus")
    } catch (e: Exception) {
        null
    }

    fun getSubscriptionEndDate(token: String): String? = try {
        decodeJwt(token).getString("subscriptionEndDate")
    } catch (e: Exception) {
        null
    }
}
