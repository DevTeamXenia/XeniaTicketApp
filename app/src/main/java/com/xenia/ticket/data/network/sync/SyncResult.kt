package com.xenia.ticket.data.network.sync


sealed class SyncResult {
    data class Success(
        val status: String = "Success",
        val message: String = ""
    ) : SyncResult()

    data class Error(
        val status: String = "Error",
        val code: Int? = null,
        val message: String = ""
    ) : SyncResult()

}