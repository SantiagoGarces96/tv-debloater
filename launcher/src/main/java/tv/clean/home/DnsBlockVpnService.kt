package tv.clean.home

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Bloqueo de anuncios SIN root, estilo Blokada: una VPN local que NO saca tu tráfico fuera.
 * Solo reenvía las consultas DNS a AdGuard DNS (94.140.14.14), que filtra anuncios y rastreadores.
 *
 * Técnica: se enruta hacia el túnel únicamente la IP del DNS; el resto del tráfico va directo
 * (no se bloquea nada por error). Se leen los paquetes DNS del túnel, se reenvían por un socket
 * protegido al resolver filtrante y se devuelve la respuesta.
 *
 * EXPERIMENTAL: maneja paquetes IPv4/UDP a mano; sin verificar en una TV real.
 */
class DnsBlockVpnService : VpnService() {

    @Volatile private var running = false
    private var vpn: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    @Volatile private var blocklist: Set<String> = emptySet()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopBlocking()
            stopSelf()
            return START_NOT_STICKY
        }
        startBlocking()
        return START_STICKY
    }

    private fun loadBlocklist(): Set<String> = runCatching {
        assets.open("blocklist.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toHashSet()
        }
    }.getOrDefault(emptySet())

    private fun startBlocking() {
        if (running) return
        if (blocklist.isEmpty()) blocklist = loadBlocklist()
        val pfd = Builder()
            .setSession("Clean Home — bloqueo de anuncios")
            .addAddress("10.111.222.2", 32)
            .addDnsServer(UPSTREAM)
            .addDnsServer(UPSTREAM2)
            .addRoute(UPSTREAM, 32)
            .addRoute(UPSTREAM2, 32)
            .setBlocking(true)
            .establish() ?: return
        vpn = pfd
        running = true
        worker = Thread { pump(pfd) }.also { it.start() }
    }

    private fun stopBlocking() {
        running = false
        runCatching { worker?.interrupt() }
        runCatching { vpn?.close() }
        vpn = null
    }

    override fun onDestroy() {
        stopBlocking()
        super.onDestroy()
    }

    private fun pump(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val socket = DatagramSocket()
        protect(socket)
        socket.soTimeout = 4000
        val packet = ByteArray(32767)
        try {
            while (running) {
                val len = input.read(packet)
                if (len <= 20) continue
                if ((packet[0].toInt() and 0xF0) != 0x40) continue // solo IPv4
                val ihl = (packet[0].toInt() and 0x0F) * 4
                if (packet[9].toInt() and 0xFF != 17) continue // solo UDP
                val srcIp = packet.copyOfRange(12, 16)
                val dstIp = packet.copyOfRange(16, 20)
                val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
                val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                if (dstPort != 53) continue
                val dnsStart = ihl + 8
                val query = packet.copyOfRange(dnsStart, len)

                val host = qname(query)
                val answer = if (host != null && isBlocked(host)) {
                    buildBlockedDns(query)        // bloqueado por lista local -> 0.0.0.0
                } else {
                    forward(socket, dstIp, query) // el resto lo filtra AdGuard
                }
                if (answer != null) {
                    // Respuesta: intercambiamos origen/destino y puertos.
                    val reply = buildUdpIpv4(dstIp, srcIp, dstPort, srcPort, answer)
                    output.write(reply)
                }
            }
        } catch (_: Throwable) {
            // túnel cerrado o interrumpido
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun forward(socket: DatagramSocket, dnsIp: ByteArray, query: ByteArray): ByteArray? = try {
        val addr = InetAddress.getByAddress(dnsIp)
        socket.send(DatagramPacket(query, query.size, addr, 53))
        val buf = ByteArray(1500)
        val resp = DatagramPacket(buf, buf.size)
        socket.receive(resp)
        buf.copyOfRange(0, resp.length)
    } catch (_: Throwable) {
        null
    }

    /** Arma un paquete IPv4+UDP con el payload DNS dado. */
    private fun buildUdpIpv4(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
    ): ByteArray {
        val udpLen = 8 + payload.size
        val totalLen = 20 + udpLen
        val p = ByteArray(totalLen)
        // IPv4
        p[0] = 0x45
        p[1] = 0
        p[2] = (totalLen shr 8).toByte(); p[3] = totalLen.toByte()
        p[4] = 0; p[5] = 0; p[6] = 0x40; p[7] = 0 // flags: don't fragment
        p[8] = 64 // TTL
        p[9] = 17 // UDP
        // checksum IP (10,11) = 0 por ahora
        System.arraycopy(srcIp, 0, p, 12, 4)
        System.arraycopy(dstIp, 0, p, 16, 4)
        val ipChecksum = checksum(p, 0, 20)
        p[10] = (ipChecksum shr 8).toByte(); p[11] = ipChecksum.toByte()
        // UDP
        val u = 20
        p[u] = (srcPort shr 8).toByte(); p[u + 1] = srcPort.toByte()
        p[u + 2] = (dstPort shr 8).toByte(); p[u + 3] = dstPort.toByte()
        p[u + 4] = (udpLen shr 8).toByte(); p[u + 5] = udpLen.toByte()
        p[u + 6] = 0; p[u + 7] = 0 // checksum UDP opcional (0 = sin checksum) en IPv4
        System.arraycopy(payload, 0, p, u + 8, payload.size)
        return p
    }

    private fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) sum += (data[i].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    /** Extrae el dominio consultado (QNAME) del paquete DNS. */
    private fun qname(dns: ByteArray): String? {
        if (dns.size < 13) return null
        var i = 12
        val sb = StringBuilder()
        while (i < dns.size) {
            val len = dns[i].toInt() and 0xFF
            if (len == 0) break
            if (len and 0xC0 != 0) return null
            i++
            if (i + len > dns.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (j in 0 until len) sb.append((dns[i + j].toInt() and 0xFF).toChar())
            i += len
        }
        val host = sb.toString().lowercase()
        return host.ifEmpty { null }
    }

    /** ¿El dominio (o alguno de sus padres) está en la lista de bloqueo? */
    private fun isBlocked(host: String): Boolean {
        if (host in blocklist) return true
        var dot = host.indexOf('.')
        while (dot >= 0 && dot + 1 < host.length) {
            if (host.substring(dot + 1) in blocklist) return true
            dot = host.indexOf('.', dot + 1)
        }
        return false
    }

    /** Fin de la sección pregunta (tras QNAME+QTYPE+QCLASS). */
    private fun questionEnd(dns: ByteArray): Int? {
        var i = 12
        while (i < dns.size) {
            val len = dns[i].toInt() and 0xFF
            if (len == 0) { i++; break }
            i += 1 + len
            if (i > dns.size) return null
        }
        i += 4
        return if (i <= dns.size) i else null
    }

    /** Respuesta DNS que apunta el dominio a 0.0.0.0 (bloqueado). */
    private fun buildBlockedDns(query: ByteArray): ByteArray? {
        val qEnd = questionEnd(query) ?: return null
        val qtype = ((query[qEnd - 4].toInt() and 0xFF) shl 8) or (query[qEnd - 3].toInt() and 0xFF)
        val out = ByteArrayOutputStream()
        out.write(query, 0, 2)          // ID
        out.write(0x81); out.write(0x80) // QR=1, RD, RA
        out.write(0x00); out.write(0x01) // QDCOUNT=1
        val isA = qtype == 1
        out.write(0x00); out.write(if (isA) 0x01 else 0x00) // ANCOUNT
        out.write(0x00); out.write(0x00) // NSCOUNT
        out.write(0x00); out.write(0x00) // ARCOUNT
        out.write(query, 12, qEnd - 12)  // sección pregunta
        if (isA) {
            out.write(0xC0); out.write(0x0C)             // puntero al nombre
            out.write(0x00); out.write(0x01)             // tipo A
            out.write(0x00); out.write(0x01)             // clase IN
            out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x2C) // TTL 300
            out.write(0x00); out.write(0x04)             // rdlength 4
            out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00) // 0.0.0.0
        }
        return out.toByteArray()
    }

    companion object {
        const val ACTION_START = "tv.clean.home.VPN_START"
        const val ACTION_STOP = "tv.clean.home.VPN_STOP"
        const val UPSTREAM = "94.140.14.14"   // AdGuard DNS (bloquea ads + rastreadores)
        const val UPSTREAM2 = "94.140.15.15"
    }
}
