package com.xenia.ticket.utils.pineLab

data class PrintRequest(
    val Header: Header,
    val Detail: Detail
)

data class Header(
    val ApplicationId: String,
    val UserId: String,
    val MethodId: String,
    val VersionNo: String
)

data class Detail(
    val PrintRefNo: String,
    val SavePrintData: Boolean,
    val Data: List<PrintData>
)

data class PrintData(
    val PrintDataType: Int,
    val PrinterWidth: Int,
    val DataToPrint: String,
    val IsCenterAligned: Boolean
)