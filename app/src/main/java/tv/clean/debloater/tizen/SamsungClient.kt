package tv.clean.debloater.tizen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Diagnóstico de conexión para Samsung Tizen.
 *
 * Samsung NO usa ADB: usa SDB (Samsung Debug Bridge) en el puerto 26101, que se abre al activar
 * el Modo Desarrollador en la TV. La API de control remoto escucha en 8001/8002.
 *
 * Este módulo SOLO comprueba conectividad y guía: la limpieza real en Tizen requiere root temporal
 * y ediciones específicas de cada modelo, que no se automatizan aquí (ver pantalla Samsung).
 */
object SamsungClient {

    const val PORT_SDB = 26101
    private const val PORT_API = 8001
    private const val PORT_API_TLS = 8002

    data class Status(
        val reachable: Boolean,
        val sdbOpen: Boolean,
        val apiOpen: Boolean,
        val detail: String,
    )

    suspend fun check(ip: String): Status = withContext(Dispatchers.IO) {
        val host = ip.trim()
        if (host.isEmpty()) {
            return@withContext Status(false, false, false, "Escribe la IP de la TV Samsung.")
        }
        val sdb = portOpen(host, PORT_SDB)
        val api = portOpen(host, PORT_API) || portOpen(host, PORT_API_TLS)
        val reachable = sdb || api
        val detail = when {
            !reachable ->
                "No responde $host. ¿Está encendida y en la misma Wi‑Fi? Para SDB hace falta el Modo Desarrollador."
            sdb ->
                "SDB (26101) abierto: Modo Desarrollador activo. Conéctate con `sdb connect $host:26101` " +
                    "y sigue la guía del proyecto para TU modelo exacto."
            else ->
                "La TV responde (API de control), pero el Modo Desarrollador (SDB 26101) no está activo. " +
                    "Actívalo en Apps con la secuencia 12345 y pon la IP de tu PC como Host PC IP."
        }
        Status(reachable = reachable, sdbOpen = sdb, apiOpen = api, detail = detail)
    }

    private fun portOpen(host: String, port: Int, timeoutMs: Int = 1200): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs); true }
    } catch (_: Throwable) {
        false
    }
}
