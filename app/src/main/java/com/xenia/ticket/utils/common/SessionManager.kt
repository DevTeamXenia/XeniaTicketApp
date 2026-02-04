package com.xenia.ticket.utils.common

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val PREF_NAME = "USER_SESSION"
        private const val KEY_IS_FIRST_LOAD = "is_first_load"
        private const val COMPANY_LANGUAGES = "SL"
        private const val STAR_ENABLE = "IS_STAR"
        private const val KEY_BILLING_SELECTED_LANGUAGE = "BSL"

        private const val KEY_SELECTED_PRINTER = "selected_printer"
    }

    fun savePineLabsAppId(appId: String) {
        editor.putString("PINE_LABS_APP_ID", appId)
        editor.apply()
    }

    fun getPineLabsAppId(): String {
        return sharedPreferences.getString("PINE_LABS_APP_ID", "") ?: ""
    }

    fun saveToken(token: String) {
        editor.putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? =
        sharedPreferences.getString(KEY_TOKEN, null)


    fun isFirstLoad(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_FIRST_LOAD, true)
    }
    fun setFirstLoad(isFirstTime: Boolean) {
        editor.putBoolean(KEY_IS_FIRST_LOAD, isFirstTime)
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun saveSelectedLanguage(languageCode: String) {
        editor.putString(COMPANY_LANGUAGES, languageCode)
        editor.apply()
    }

    fun saveIsStar(isStar: Boolean) {
        editor.putBoolean(STAR_ENABLE, isStar)
        editor.apply()
    }

    fun savePassword(password: String) {
        sharedPreferences.edit().putString("PASSWORD", password).apply()
    }

    fun getPassword(): String? {
        return sharedPreferences.getString("PASSWORD", null)
    }

    fun clearPassword() {
        sharedPreferences.edit().remove("PASSWORD").apply()
    }

    fun requireToken(): String {
        return getToken() ?: throw IllegalStateException("Token missing")
    }

    fun getUserType(): String {
        return JwtUtils.getUserType(requireToken()) ?: "UNKNOWN"
    }


    fun getUserId(): Int {
        return JwtUtils.getUserId(requireToken())
            ?: throw IllegalStateException("UserId missing in JWT")
    }

    fun getSelectedLanguage(): String {
        return sharedPreferences.getString(COMPANY_LANGUAGES, "en") ?: "en"
    }


    fun saveBillingSelectedLanguage(languageCode: String) {
        editor.putString(KEY_BILLING_SELECTED_LANGUAGE, languageCode)
        editor.apply()
    }

    fun getBillingSelectedLanguage(): String {
        return sharedPreferences.getString(KEY_BILLING_SELECTED_LANGUAGE, "en") ?: "en"
    }

    fun saveSelectedPrinter(printer: String) {
        editor.putString(KEY_SELECTED_PRINTER, printer)
        editor.apply()
    }

    fun getSelectedPrinter(): String? {
        return sharedPreferences.getString(KEY_SELECTED_PRINTER, null)
    }

    fun clearSession() {
        editor.clear()
        editor.apply()
    }
}