package com.xenia.ticket.data.network.model

sealed class DisplayItem {
    data class Header(val title: String) : DisplayItem()
    data object ColumnHeader : DisplayItem()
    data class Item(
        val name: String,
        val qty: Double,
        val rate: Double,
        val total: Double
    ) : DisplayItem()

    data class TotalRow( val totalAmount: Double) : DisplayItem()
}