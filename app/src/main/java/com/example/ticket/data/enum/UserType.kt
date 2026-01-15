package com.example.ticket.data.enum

import kotlin.collections.find
import kotlin.text.equals

enum class UserType(val value: String) {
    CUSTOMER("User"),
    COUNTER_USER("CounterUser"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String?): UserType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
