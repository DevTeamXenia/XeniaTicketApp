package com.example.ticket.data.local



sealed class SyncResult {
    data class Success(val message: String = "") : SyncResult()
    data class Error(val error: String = "") : SyncResult()
}
