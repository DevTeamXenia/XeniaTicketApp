package com.example.ticket.data.room.entity



import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class Ticket(
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
    val ticketAmount: Double,
    val ticketCreatedDate: String,
    val ticketCreatedBy: Int,
    val ticketActive: Boolean,

    // Optional cart fields for in-app cart functionality
    var cartQty: Int = 0,
    var cartTotalAmount: Double = 0.0,
    var cartUserName: String = "",
    var cartUserPhone: String = "",
    var cartUserIdNo: String = "",
    var cartUserProof: String = "",
    var cartUserImg: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ticket) return false

        return id == other.id &&
                ticketId == other.ticketId &&
                ticketName == other.ticketName &&
                ticketNameMa == other.ticketNameMa &&
                ticketNameTa == other.ticketNameTa &&
                ticketNameTe == other.ticketNameTe &&
                ticketNameKa == other.ticketNameKa &&
                ticketNameHi == other.ticketNameHi &&
                ticketNamePa == other.ticketNamePa &&
                ticketNameMr == other.ticketNameMr &&
                ticketNameSi == other.ticketNameSi &&
                ticketCategoryId == other.ticketCategoryId &&
                ticketCompanyId == other.ticketCompanyId &&
                ticketAmount == other.ticketAmount &&
                ticketCreatedDate == other.ticketCreatedDate &&
                ticketCreatedBy == other.ticketCreatedBy &&
                ticketActive == other.ticketActive &&
                cartQty == other.cartQty &&
                cartTotalAmount == other.cartTotalAmount &&
                cartUserName == other.cartUserName &&
                cartUserPhone == other.cartUserPhone &&
                cartUserIdNo == other.cartUserIdNo &&
                cartUserProof == other.cartUserProof &&
                (cartUserImg?.contentEquals(other.cartUserImg ?: ByteArray(0)) ?: (other.cartUserImg == null))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + ticketId
        result = 31 * result + ticketName.hashCode()
        result = 31 * result + (ticketNameMa?.hashCode() ?: 0)
        result = 31 * result + (ticketNameTa?.hashCode() ?: 0)
        result = 31 * result + (ticketNameTe?.hashCode() ?: 0)
        result = 31 * result + (ticketNameKa?.hashCode() ?: 0)
        result = 31 * result + (ticketNameHi?.hashCode() ?: 0)
        result = 31 * result + (ticketNamePa?.hashCode() ?: 0)
        result = 31 * result + (ticketNameMr?.hashCode() ?: 0)
        result = 31 * result + (ticketNameSi?.hashCode() ?: 0)
        result = 31 * result + ticketCategoryId
        result = 31 * result + ticketCompanyId
        result = 31 * result + ticketAmount.hashCode()
        result = 31 * result + ticketCreatedDate.hashCode()
        result = 31 * result + ticketCreatedBy
        result = 31 * result + ticketActive.hashCode()
        result = 31 * result + cartQty
        result = 31 * result + cartTotalAmount.hashCode()
        result = 31 * result + cartUserName.hashCode()
        result = 31 * result + cartUserPhone.hashCode()
        result = 31 * result + cartUserIdNo.hashCode()
        result = 31 * result + cartUserProof.hashCode()
        result = 31 * result + (cartUserImg?.contentHashCode() ?: 0)
        return result
    }
}

