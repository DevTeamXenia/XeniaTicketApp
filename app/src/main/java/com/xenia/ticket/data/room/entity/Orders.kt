package com.xenia.ticket.data.room.entity
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Orders")
data class Orders(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val ticketId: Int,
    val ticketName: String,
    val ticketNameMa: String?,
    val ticketNameTa: String?,
    val ticketNameTe: String?,
    val ticketNameKa: String?,
    val ticketNameHi: String?,
    val ticketNamePa: String?,
    val ticketNameMr: String?,
    val ticketNameSi: String?,
    val ticketCategoryId: Int,
    val ticketCompanyId: Int,
    val ticketTotalAmount: Double,
    val ticketCreatedDate: String,
    val ticketCreatedBy: Int,
    val ticketActive: Boolean,
    val ticketCombo: Boolean,
    val ticketType: String,
    val daName: String,
    val ticketRate: Double,
    val ticketChildRate: Double,
    val ticketQty: Int,
    val ticketChildQty: Int,
    val ticketChild: Boolean,
    val daPhoneNumber: String,
    val daCustRefNo: String,
    val daNpciTransId: String,
    val daProofId: String,
    val daProof: String,
    val daImg: ByteArray?,
    val screenId: Int,
    val scheduleId: Int,
    val scheduleDay: String,
    val scheduleTime: String,
    val screenName: String,
)


