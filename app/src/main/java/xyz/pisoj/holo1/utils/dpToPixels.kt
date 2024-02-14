package xyz.pisoj.holo1.utils

import android.content.Context

fun Context.dpToPixels(dp: Int): Int {
    val scale: Float = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}