package com.xenia.ticket.data.network.model

data class ActiveItem(
    val id: Int,
    val name: String,
    val nameMa: String?,
    val nameTa: String?,
    val nameTe: String?,
    val nameKa: String?,
    val nameHi: String?,
    val namePa: String?,
    val nameSi: String?,
    val nameMr: String?,
    val category: Int,
    val companyId: Int,
    val amount: Double,
    val active: Boolean,
    val combo: Boolean,
    val type: String
)