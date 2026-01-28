enum class UserType(val value: String) {
    CUSTOMER("User"),
    COUNTER_USER("CounterUser"),
    PROCESS_USER("ProcessUser"),
    UNKNOWN("unknown");

    companion object {

        fun fromValue(value: String?): UserType {
            if (value.isNullOrBlank()) return UNKNOWN

            val normalized = value.trim()

            return entries.firstOrNull {
                it.value.equals(normalized, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}
