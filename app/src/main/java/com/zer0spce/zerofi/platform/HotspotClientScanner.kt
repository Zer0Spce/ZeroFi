package com.zer0spce.zerofi.platform

import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

data class HotspotClient(
    val ipAddress: String,
    val macAddress: String,
    val interfaceName: String,
)

data class HotspotClientSnapshot(
    val available: Boolean = false,
    val clients: List<HotspotClient> = emptyList(),
    val source: String = "Android restricted",
    val estimatedCount: Int? = null,
)

/**
 * Best-effort tethered-client discovery.
 *
 * Detection order:
 * 1. Kernel ARP table when the OEM exposes it.
 * 2. `ip neigh` / netlink neighbor table when ARP is hidden but route-neighbor
 *    information is still available to ordinary apps.
 *
 * Modern Android/OEM builds can intentionally hide both sources. In that case
 * available remains false; callers must never invent an authoritative count.
 */
object HotspotClientScanner {
    fun scan(): HotspotClientSnapshot {
        val tetherInterfaces = candidateTetherInterfaces()

        readArp(tetherInterfaces)?.let { return it }
        readNeighborTable(tetherInterfaces)?.let { return it }

        return HotspotClientSnapshot()
    }

    private fun readArp(candidateInterfaces: Set<String>): HotspotClientSnapshot? = runCatching {
        val arp = File("/proc/net/arp")
        if (!arp.canRead()) return@runCatching null
        val lines = arp.readLines()
        if (lines.isEmpty() || !lines.first().contains("IP address", ignoreCase = true)) {
            return@runCatching null
        }

        val clients = lines.drop(1).mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 6) return@mapNotNull null
            val ip = parts[0]
            val flags = parts[2]
            val mac = parts[3].lowercase()
            val iface = parts[5]
            if (flags != "0x2" || mac == "00:00:00:00:00:00" || ip == "0.0.0.0") return@mapNotNull null
            if (!isPrivateIpv4(ip) || !isCandidateInterface(iface, candidateInterfaces)) return@mapNotNull null
            HotspotClient(ip, mac, iface)
        }.distinctBy { it.macAddress }

        HotspotClientSnapshot(
            available = true,
            clients = clients,
            source = "ARP table",
        )
    }.getOrNull()

    private fun readNeighborTable(candidateInterfaces: Set<String>): HotspotClientSnapshot? = runCatching {
        val ipBinary = listOf("/system/bin/ip", "/vendor/bin/ip", "ip").firstOrNull { path ->
            path == "ip" || File(path).canExecute()
        } ?: return@runCatching null

        val process = ProcessBuilder(ipBinary, "neigh", "show")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        if (exit != 0) return@runCatching null

        val clients = output.lineSequence().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 3) return@mapNotNull null
            val ip = parts.firstOrNull() ?: return@mapNotNull null
            val devIndex = parts.indexOf("dev")
            val lladdrIndex = parts.indexOf("lladdr")
            if (devIndex < 0 || devIndex + 1 >= parts.size || lladdrIndex < 0 || lladdrIndex + 1 >= parts.size) {
                return@mapNotNull null
            }
            val iface = parts[devIndex + 1]
            val mac = parts[lladdrIndex + 1].lowercase()
            val state = parts.lastOrNull()?.uppercase().orEmpty()
            if (state == "FAILED" || state == "INCOMPLETE") return@mapNotNull null
            if (mac == "00:00:00:00:00:00" || !MAC_REGEX.matches(mac)) return@mapNotNull null
            if (!isPrivateIpv4(ip) || !isCandidateInterface(iface, candidateInterfaces)) return@mapNotNull null
            HotspotClient(ip, mac, iface)
        }.distinctBy { it.macAddress }
            .toList()

        HotspotClientSnapshot(
            available = true,
            clients = clients,
            source = "Neighbor table",
        )
    }.getOrNull()

    private fun candidateTetherInterfaces(): Set<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().mapNotNull { network ->
            if (!network.isUp || network.isLoopback) return@mapNotNull null
            val addresses = network.interfaceAddresses
            val hasPrivateGatewayStyleAddress = addresses.any { interfaceAddress ->
                val address = interfaceAddress.address as? Inet4Address ?: return@any false
                val octets = address.address.map { it.toInt() and 0xff }
                val private = when {
                    octets[0] == 10 -> true
                    octets[0] == 172 && octets[1] in 16..31 -> true
                    octets[0] == 192 && octets[1] == 168 -> true
                    else -> false
                }
                private && octets[3] == 1
            }
            if (hasPrivateGatewayStyleAddress || looksLikeTetherInterface(network.name)) network.name else null
        }.toSet()
    }.getOrDefault(emptySet())

    private fun isCandidateInterface(iface: String, candidates: Set<String>): Boolean {
        if (candidates.isNotEmpty()) return iface in candidates
        return looksLikeTetherInterface(iface)
    }

    private fun looksLikeTetherInterface(name: String): Boolean {
        val value = name.lowercase()
        if (value.startsWith("rmnet") || value.startsWith("ccmni") || value.startsWith("pdp") || value.startsWith("tun")) return false
        return value.contains("wlan") || value.contains("ap") || value.contains("softap") ||
            value.contains("swlan") || value.contains("tether") || value.contains("rndis") ||
            value.contains("ncm") || value.contains("usb") || value.contains("bt-pan")
    }

    private fun isPrivateIpv4(value: String): Boolean {
        val p = value.split('.')
        if (p.size != 4) return false
        val n = p.mapNotNull { it.toIntOrNull() }
        if (n.size != 4 || n.any { it !in 0..255 }) return false
        return n[0] == 10 || (n[0] == 172 && n[1] in 16..31) || (n[0] == 192 && n[1] == 168)
    }

    private val MAC_REGEX = Regex("^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")
}
