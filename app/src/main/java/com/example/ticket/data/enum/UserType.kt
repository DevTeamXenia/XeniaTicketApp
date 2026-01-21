enum class UserType(val value: String) {
    CUSTOMER("User"),
    COUNTER_USER("CounterUser"),
    PROCESS_USER("ProcessUser"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String?): UserType {
            return entries.find {
                it.value.equals(value, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}
