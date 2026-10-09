package com.lyrnox.lxvideo.util

import java.util.Locale

fun fmtSize(n: Long): String {
    if (n < 1024) return "$n B"
    val u = arrayOf("KB", "MB", "GB", "TB")
    var v = n.toDouble()
    var i = -1
    do {
        v /= 1024; i++
    } while (v >= 1024 && i < u.size - 1)
    val s = if (v >= 100) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
    return "$s ${u[i]}"
}
