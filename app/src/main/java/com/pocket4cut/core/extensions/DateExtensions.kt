package com.pocket4cut.core.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Long (epoch millis)을 날짜 문자열로 변환
 */
fun Long.toDateString(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

/**
 * 현재 시간을 epoch millis로 반환
 */
fun currentTimeMillis(): Long = System.currentTimeMillis()

