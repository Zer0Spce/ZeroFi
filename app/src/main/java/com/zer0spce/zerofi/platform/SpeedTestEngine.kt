package com.zer0spce.zerofi.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToLong

data class SpeedTestResult(
    val latencyMs: Long? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val error: String? = null,
)

object SpeedTestEngine {
    /** Small manual test: roughly 2 MB download + 512 KB upload. */
    suspend fun run(): SpeedTestResult = withContext(Dispatchers.IO) {
        runCatching {
            val latency = measureLatency()
            val down = measureDownload()
            val up = measureUpload()
            SpeedTestResult(latency, down, up)
        }.getOrElse { SpeedTestResult(error = it.message ?: "Speed test failed") }
    }

    private fun measureLatency(): Long {
        val started = System.nanoTime()
        val connection = (URL("https://speed.cloudflare.com/__down?bytes=0").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            useCaches = false
            requestMethod = "GET"
        }
        try {
            connection.inputStream.use { it.read() }
            if (connection.responseCode !in 200..299) error("Latency test HTTP ${connection.responseCode}")
        } finally {
            connection.disconnect()
        }
        return ((System.nanoTime() - started) / 1_000_000.0).roundToLong()
    }

    private fun measureDownload(): Double {
        val bytesRequested = 2_000_000
        val connection = (URL("https://speed.cloudflare.com/__down?bytes=$bytesRequested").openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 12_000
            useCaches = false
            requestMethod = "GET"
        }
        var total = 0L
        val started = System.nanoTime()
        try {
            BufferedInputStream(connection.inputStream).use { input ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                }
            }
            if (connection.responseCode !in 200..299) error("Download test HTTP ${connection.responseCode}")
        } finally {
            connection.disconnect()
        }
        val seconds = (System.nanoTime() - started) / 1_000_000_000.0
        return if (seconds > 0.0) total * 8.0 / seconds / 1_000_000.0 else 0.0
    }

    private fun measureUpload(): Double {
        val payload = ByteArray(512 * 1024) { 0x5A.toByte() }
        val connection = (URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 10_000
            useCaches = false
            doOutput = true
            requestMethod = "POST"
            setFixedLengthStreamingMode(payload.size)
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        val started = System.nanoTime()
        try {
            connection.outputStream.use { it.write(payload) }
            connection.inputStream.use { input -> while (input.read() != -1) Unit }
            if (connection.responseCode !in 200..299) error("Upload test HTTP ${connection.responseCode}")
        } finally {
            connection.disconnect()
        }
        val seconds = (System.nanoTime() - started) / 1_000_000_000.0
        return if (seconds > 0.0) payload.size * 8.0 / seconds / 1_000_000.0 else 0.0
    }
}
