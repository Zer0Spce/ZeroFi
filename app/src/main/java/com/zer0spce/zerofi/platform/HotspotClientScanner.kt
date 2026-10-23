package com.zer0spce.zerofi.platform

import java.io.File

data class HotspotClient(
    val ipAddress: String,
    val macAddress: String,
    val interfaceName: String,
)

data class HotspotClientSnapshot(
    val available: Boolean = false,
    val clients: List<HotspotClient> = emptyList(),
    val source: String = "Android restricted",
)

/**
 * Best-effort client discovery for OEMs that still expose the kernel ARP table.
 * Modern Android may intentionally hide it; callers must respect available=false
 * and never invent a client count.
 */
object HotspotClientScanner {
    fun scan(): HotspotClientSnapshot {
        return runCatching {
            val arp = File("/proc/net/arp")
            if (!arp.canRead()) return HotspotClientSnapshot()
            val lines = arp.readLines()
            if (lines.isEmpty() || !lines.first().contains("IP address", ignoreCase = true)) {
                return HotspotClientSnapshot()
            }
            val clients = lines.drop(1).mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 6) return@mapNotNull null
                val ip = parts[0]
                val flags = parts[2]
                val mac = parts[3].lowercase()
                val iface = parts[5]
                if (flags != "0x2" || mac == "00:00:00:00:00:00" || ip == "0.0.0.0") return@mapNotNull null
                HotspotClient(ip, mac, iface)
            }.distinctBy { it.macAddress }
            HotspotClientSnapshot(true, clients, "ARP table")
        }.getOrElse { HotspotClientSnapshot() }
    }
}
