package com.truthscan.app.util

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageTracker(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("truthscan_usage", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun canUseToday(): Boolean {
        val today = dateFormat.format(Date())
        val count = getUsageCount(today)
        return count < DAILY_FREE_LIMIT
    }

    fun incrementUsageCount() {
        val today = dateFormat.format(Date())
        val currentCount = getUsageCount(today)
        prefs.edit().putInt("usage_$today", currentCount + 1).apply()
    }

    fun getUsageCount(date: String? = null): Int {
        val key = if (date != null) "usage_$date" else "usage_${dateFormat.format(Date())}"
        return prefs.getInt(key, 0)
    }

    companion object {
        private const val DAILY_FREE_LIMIT = 5
    }
}
