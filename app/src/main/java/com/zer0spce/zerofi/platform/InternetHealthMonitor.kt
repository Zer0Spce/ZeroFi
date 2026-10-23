package com.zer0spce.zerofi.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class InternetHealthSnapshot(
    val checkedAt: Long = 0L,
    val dnsOk: Boolean = false,
    val tcpOk: Boolean = false,
    val httpOk: Boolean = false,
    val latencyMs: Int? = null,
    val packetLossPercent: Int? = null,
    val summary: String = "Not checked",
)

object InternetHealthMonitor {
    suspend fun check(): InternetHealthSnapshot = withContext(Dispatchers.IO) {
        val checkedAt = System.currentTimeMillis()
        val dnsOk = runCatching { InetAddress.getByName("one.one.one.one") }.isSuccess

        val samples = mutableListOf<Long>()
        var failures = 0
        repeat(3) {
            val started = System.nanoTime()
            val ok = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("1.1.1.1", 443), 1_500)
                }
            }.isSuccess
            if (ok) samples += (System.nanoTime() - started) / 1_000_000L else failures++
        }
        val tcpOk = samples.isNotEmpty()
        val latency = samples.sorted().let { values -> if (values.isEmpty()) null else values[values.size / 2].toInt() }
        val loss = ((failures / 3.0) * 100.0).roundToInt()

        val httpOk = runCatching {
            val connection = URL("https://connectivitycheck.gstatic.com/generate_204").openConnection() as HttpsURLConnection
            connection.connectTimeout = 2_500
            connection.readTimeout = 2_500
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            val code = connection.responseCode
            connection.disconnect()
            code in 200..399
        }.getOrDefault(false)

        val summary = when {
            dnsOk && tcpOk && httpOk && loss == 0 -> "Healthy"
            tcpOk && httpOk -> "Online with minor issues"
            tcpOk && !httpOk -> "Internet path reachable, validation failed"
            dnsOk && !tcpOk -> "DNS works, Internet path is failing"
            !dnsOk && tcpOk -> "Internet reachable, DNS appears unhealthy"
            else -> "Internet unavailable"
        }
        InternetHealthSnapshot(checkedAt, dnsOk, tcpOk, httpOk, latency, loss, summary)
    }
}
