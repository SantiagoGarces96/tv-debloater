package tv.clean.debloater.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Descubre TVs en la red local escaneando el puerto ADB (5555) de la subred /24 del teléfono.
 * Es más fiable que mDNS para encontrar ADB, porque muchas Android TV abren 5555 sin anunciarlo.
 */
object TvDiscovery {

    const val ADB_PORT = 5555

    /** IPs de la subred con el puerto 5555 abierto. */
    suspend fun scan(timeoutMs: Int = 350): List<String> = withContext(Dispatchers.IO) {
        val base = localSubnetBase() ?: return@withContext emptyList()
        val limit = Semaphore(64)
        coroutineScope {
            (1..254).map { i ->
                async {
                    limit.withPermit {
                        val ip = base + i
                        if (portOpen(ip, ADB_PORT, timeoutMs)) ip else null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun portOpen(host: String, port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    } catch (_: Throwable) {
        false
    }

    /** Devuelve "192.168.1." (prefijo /24) a partir de la IPv4 local no de loopback. */
    private fun localSubnetBase(): String? {
        val ip = localIpv4() ?: return null
        val lastDot = ip.lastIndexOf('.')
        return if (lastDot > 0) ip.substring(0, lastDot + 1) else null
    }

    private fun localIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { addr ->
                    !addr.isLoopbackAddress &&
                        addr.hostAddress?.contains('.') == true &&
                        addr.isSiteLocalAddress
                }
                ?.hostAddress
        } catch (_: Throwable) {
            null
        }
    }
}
