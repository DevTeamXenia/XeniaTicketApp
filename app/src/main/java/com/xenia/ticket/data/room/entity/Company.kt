package com.xenia.ticket.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "CompanySettings")
data class Company(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val keyCode: String,
    val value: String?,

    val applicationId: String?
)




