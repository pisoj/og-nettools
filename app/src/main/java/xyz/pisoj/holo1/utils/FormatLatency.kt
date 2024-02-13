package xyz.pisoj.holo1.utils

fun formatLatencyMillis(latencyMillis: Long): String {
    return if(latencyMillis > 1000) {
        "${"%.2f".format(latencyMillis/1000f)}s"
    } else {
        "${latencyMillis}ms"
    }
}