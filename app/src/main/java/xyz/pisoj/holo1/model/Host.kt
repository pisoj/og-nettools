package xyz.pisoj.holo1.model

data class Host(
    val host: String,
    val latencyMillis: Long?,
    val status: Status
) {
    enum class Status {
        Available,
        Warning,
        Error,
        Unavailable,
    }
}
