package com.zer0spce.zerofi.data

import android.content.Context
import com.zer0spce.zerofi.platform.SpeedTestResult
import org.json.JSONArray
import org.json.JSONObject

data class SpeedHistoryEntry(
    val timestampMs: Long,
    val latencyMs: Long?,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
)

class SpeedHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("zerofi_speed_history", Context.MODE_PRIVATE)

    fun entries(): List<SpeedHistoryEntry> = runCatching {
        val array = JSONArray(prefs.getString(KEY_HISTORY, "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SpeedHistoryEntry(
                        timestampMs = item.optLong("time"),
                        latencyMs = item.optLong("ping").takeIf { item.has("ping") },
                        downloadMbps = item.optDouble("down").takeIf { item.has("down") },
                        uploadMbps = item.optDouble("up").takeIf { item.has("up") },
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun add(result: SpeedTestResult) {
        if (result.error != null) return
        val current = entries().toMutableList()
        current.add(
            0,
            SpeedHistoryEntry(
                timestampMs = System.currentTimeMillis(),
                latencyMs = result.latencyMs,
                downloadMbps = result.downloadMbps,
                uploadMbps = result.uploadMbps,
            )
        )
        val array = JSONArray()
        current.take(MAX_ENTRIES).forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("time", entry.timestampMs)
                    entry.latencyMs?.let { put("ping", it) }
                    entry.downloadMbps?.let { put("down", it) }
                    entry.uploadMbps?.let { put("up", it) }
                }
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clear() = prefs.edit().remove(KEY_HISTORY).apply()

    companion object {
        private const val KEY_HISTORY = "history"
        private const val MAX_ENTRIES = 20
    }
}
