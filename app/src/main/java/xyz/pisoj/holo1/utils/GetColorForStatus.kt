package xyz.pisoj.holo1.utils

import android.content.Context
import android.os.Build
import xyz.pisoj.holo1.R
import xyz.pisoj.holo1.model.Host

fun Context.getColorForStatus(status: Host.Status): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        resources.getColor(
            when(status) {
                Host.Status.Available -> R.color.available
                Host.Status.Warning -> R.color.warning
                Host.Status.Error -> R.color.error
                Host.Status.Unavailable -> R.color.unavailable
            },
            null
        )
    } else {
        resources.getColor(
            when(status) {
                Host.Status.Available -> R.color.available
                Host.Status.Warning -> R.color.warning
                Host.Status.Error -> R.color.error
                Host.Status.Unavailable -> R.color.unavailable
            }
        )
    }
}