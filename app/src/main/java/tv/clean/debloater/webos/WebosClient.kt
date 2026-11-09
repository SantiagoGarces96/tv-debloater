package tv.clean.debloater.webos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * Cliente mínimo para LG webOS en Modo Desarrollador (sin root).
 *
 * webOS NO es Android: no hay ADB. El "no-root" expone:
 *  - 9922: servidor SSH del Modo Desarrollador (para instalar el IPK del launcher).
 *  - 9990/9991: key server que entrega la clave privada a partir de la passphrase de 6 caracteres.
 *  - 3000/3001: SSAP (control por WebSocket).
 *
 * Este módulo cubre el diagnóstico de conexión y la obtención de la clave.
 * La instalación del IPK por SSH es el siguiente incremento.
 */
object WebosClient {

    const val PORT_SSH = 9922
    const val PORT_SSAP = 3000
    private val KEY_SERVER_PORTS = listOf(9991, 9990)

    data class Status(
        val reachable: Boolean,
        val sshOpen: Boolean,
        val ssapOpen: Boolean,
        val keyServerPort: Int?,
        val devKeyPem: String?,
        val detail: String,
    )

    suspend fun check(ip: String, passphrase: String): Status = withContext(Dispatchers.IO) {
        val host = ip.trim()
        if (host.isEmpty()) {
            return@withContext Status(false, false, false, null, null, "Escribe la IP de la TV LG.")
        }

        val ssap = portOpen(host, PORT_SSAP)
        val ssh = portOpen(host, PORT_SSH)
        val keyPort = KEY_SERVER_PORTS.firstOrNull { portOpen(host, it) }
        val reachable = ssap || ssh || keyPort != null

        if (!reachable) {
            return@withContext Status(
                false, false, false, null, null,
                "No responde $host. ¿Está encendida, en la misma Wi‑Fi y con Modo Desarrollador abierto?",
            )
        }
        if (ssh.not() && keyPort == null) {
            return@withContext Status(
                true, false, ssap, null, null,
                "La TV responde, pero el Modo Desarrollador (SSH/Key Server) no está activo. " +
                    "Abre la app Developer Mode en la TV y enciende Dev Mode Status y Key Server.",
            )
        }

        var key: String? = null
        var keyDetail = ""
        if (keyPort != null && passphrase.isNotBlank()) {
            key = fetchDevKey(host, keyPort, passphrase.trim())
            keyDetail = if (key != null) {
                "Clave de Modo Desarrollador obtenida (${key.length} bytes)."
            } else {
                "No se pudo obtener la clave: revisa la passphrase de 6 caracteres (respeta mayúsculas)."
            }
        } else if (passphrase.isBlank()) {
            keyDetail = "Introduce la passphrase de 6 caracteres que muestra la app Developer Mode."
        }

        Status(
            reachable = true,
            sshOpen = ssh,
            ssapOpen = ssap,
            keyServerPort = keyPort,
            devKeyPem = key,
            detail = buildString {
                append("Conexión OK. ")
                append("SSH ${if (ssh) "abierto" else "cerrado"}, ")
                append("Key Server ${if (keyPort != null) "en $keyPort" else "no visible"}. ")
                append(keyDetail)
            }.trim(),
        )
    }

    /** GET http://ip:port/webos_rsa?passphrase=XXXXXX → clave privada PEM. */
    private fun fetchDevKey(ip: String, port: Int, passphrase: String): String? = try {
        val url = URL("http://$ip:$port/webos_rsa?passphrase=$passphrase")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
        }
        try {
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    .takeIf { it.contains("PRIVATE KEY") }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    } catch (_: Throwable) {
        null
    }

    private fun portOpen(host: String, port: Int, timeoutMs: Int = 1200): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs); true }
    } catch (_: Throwable) {
        false
    }
}
