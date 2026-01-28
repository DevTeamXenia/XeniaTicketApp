package com.xenia.ticket.data.network.local



sealed class SyncResult {
    data class Success(val message: String = "") : SyncResult()
    data class Error(val error: String = "") : SyncResult()
}
