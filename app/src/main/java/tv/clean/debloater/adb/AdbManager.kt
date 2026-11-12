package tv.clean.debloater.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Servicio "cerrar avisos" que vive dentro del launcher SG Alfa. */
const val AUTOCLICK_COMPONENT = "tv.clean.home/tv.clean.home.AutoClickService"

/** Nombre de paquete Android válido (evita inyectar cosas en comandos de shell). */
private val PKG_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

/** Resultado simple para no propagar excepciones a la UI. */
sealed interface AdbResult<out T> {
    data class Ok<T>(val value: T) : AdbResult<T>
    data class Err(val message: String) : AdbResult<Nothing>
}

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdk: String,
    /** "Televisor", "Decodificador / TV box", "Fire TV" o "Dispositivo Android". */
    val kind: String = "Dispositivo Android",
)

/**
 * Envuelve dadb para hablar ADB por TCP con la TV.
 *
 * - Genera y guarda una clave ADB propia en filesDir/adb (persistente), de modo que la TV
 *   puede recordar "Permitir siempre desde este dispositivo".
 * - Toda operación de red va en Dispatchers.IO.
 */
class AdbManager(context: Context) {

    private val keyDir = File(context.filesDir, "adb").apply { mkdirs() }
    private val privateKeyFile = File(keyDir, "adbkey")
    private val publicKeyFile = File(keyDir, "adbkey.pub")

    @Volatile
    private var dadb: Dadb? = null

    @Volatile
    var host: String? = null
        private set

    val isConnected: Boolean get() = dadb != null

    private fun keyPair(): AdbKeyPair {
        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            AdbKeyPair.generate(privateKeyFile, publicKeyFile)
        }
        return AdbKeyPair.read(privateKeyFile, publicKeyFile)
    }

    suspend fun connect(host: String, port: Int = 5555): AdbResult<DeviceInfo> =
        withContext(Dispatchers.IO) {
            close()
            try {
                val connection = Dadb.create(host, port, keyPair())
                // Fuerza el handshake/autenticación con un comando trivial.
                val probe = connection.shell("echo ok")
                if (probe.exitCode != 0) {
                    connection.close()
                    return@withContext AdbResult.Err(
                        "La TV respondió con código ${probe.exitCode}. ¿Aceptaste el aviso de depuración?",
                    )
                }
                dadb = connection
                this@AdbManager.host = host
                AdbResult.Ok(readDeviceInfo(connection))
            } catch (e: Throwable) {
                close()
                AdbResult.Err(describe(e, host, port))
            }
        }

    private fun readDeviceInfo(connection: Dadb): DeviceInfo {
        fun prop(name: String): String =
            runCatching { connection.shell("getprop $name").output.trim() }.getOrDefault("")
        val model = prop("ro.product.model").ifBlank { "Desconocido" }
        val manufacturer = prop("ro.product.manufacturer")
        val characteristics = prop("ro.build.characteristics").lowercase()
        val features = runCatching { connection.shell("pm list features").output }.getOrDefault("")
        val leanback = features.contains("android.software.leanback")
        val isTvType = features.contains("android.hardware.type.television") ||
            characteristics.contains("tv") || leanback
        val hasTuner = features.contains("android.hardware.tv.tuner") ||
            features.contains("android.software.live_tv")
        val isFire = manufacturer.contains("amazon", true) || model.startsWith("AFT", true)
        val kind = when {
            isFire -> "Fire TV"
            isTvType && hasTuner -> "Televisor (Android TV)"
            isTvType -> "Decodificador / TV box"
            else -> "Dispositivo Android"
        }
        return DeviceInfo(
            model = model,
            manufacturer = manufacturer,
            androidVersion = prop("ro.build.version.release"),
            sdk = prop("ro.build.version.sdk"),
            kind = kind,
        )
    }

    /** Devuelve los nombres de paquete instalados para el usuario 0. */
    suspend fun listPackages(): AdbResult<List<String>> = shellLines("pm list packages --user 0") { lines ->
        lines.mapNotNull { line ->
            line.trim().removePrefix("package:").takeIf { it.isNotBlank() }
        }.sorted()
    }

    /** pm uninstall --user 0 <pkg>. Reversible con reinstallExisting. */
    suspend fun uninstallForUser0(pkg: String): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
            try {
                val r = d.shell("pm uninstall --user 0 $pkg")
                val out = (r.output + r.errorOutput).trim()
                if (out.contains("Success", ignoreCase = true)) {
                    AdbResult.Ok(Unit)
                } else {
                    AdbResult.Err(out.ifBlank { "Falló al desinstalar $pkg" })
                }
            } catch (e: Throwable) {
                AdbResult.Err(describe(e, host, 5555))
            }
        }

    /** pm disable-user --user 0 <pkg>. No borra nada; reversible con enableForUser0. */
    suspend fun disableForUser0(pkg: String): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
            try {
                val r = d.shell("pm disable-user --user 0 $pkg")
                val out = (r.output + r.errorOutput).trim()
                if (out.contains("disabled", ignoreCase = true)) {
                    AdbResult.Ok(Unit)
                } else {
                    AdbResult.Err(out.ifBlank { "Falló al desactivar $pkg" })
                }
            } catch (e: Throwable) {
                AdbResult.Err(describe(e, host, 5555))
            }
        }

    /** Revierte un disable: pm enable <pkg>. */
    suspend fun enableForUser0(pkg: String): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
            try {
                val r = d.shell("pm enable $pkg")
                val out = (r.output + r.errorOutput).trim()
                if (out.contains("enabled", ignoreCase = true) || r.exitCode == 0) {
                    AdbResult.Ok(Unit)
                } else {
                    AdbResult.Err(out.ifBlank { "Falló al activar $pkg" })
                }
            } catch (e: Throwable) {
                AdbResult.Err(describe(e, host, 5555))
            }
        }

    /** Revierte: vuelve a habilitar el APK de sistema para el usuario 0. */
    suspend fun reinstallExisting(pkg: String): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
            try {
                val r = d.shell("cmd package install-existing --user 0 $pkg")
                val out = (r.output + r.errorOutput).trim()
                if (out.contains("installed", ignoreCase = true) || r.exitCode == 0) {
                    AdbResult.Ok(Unit)
                } else {
                    AdbResult.Err(out.ifBlank { "Falló al restaurar $pkg" })
                }
            } catch (e: Throwable) {
                AdbResult.Err(describe(e, host, 5555))
            }
        }

    /** Instala un APK (p. ej. el launcher) en la TV. */
    suspend fun installApk(apk: File): AdbResult<Unit> = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
        try {
            d.install(apk)
            AdbResult.Ok(Unit)
        } catch (e: Throwable) {
            AdbResult.Err(describe(e, host, 5555))
        }
    }

    /** Fija la pantalla de inicio por defecto (componente launcher). */
    suspend fun setDefaultHome(component: String): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
            try {
                val r = d.shell("cmd package set-home-activity $component")
                val out = (r.output + r.errorOutput).trim()
                if (r.exitCode == 0 || out.contains("Success", ignoreCase = true)) {
                    AdbResult.Ok(Unit)
                } else {
                    AdbResult.Err(out.ifBlank { "No se pudo fijar el launcher" })
                }
            } catch (e: Throwable) {
                AdbResult.Err(describe(e, host, 5555))
            }
        }

    /** Abre en la tele la ficha oficial de Play Store de un paquete. */
    suspend fun openPlayStore(pkg: String): AdbResult<Unit> = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
        if (!PKG_REGEX.matches(pkg)) return@withContext AdbResult.Err("Nombre de paquete no válido.")
        try {
            val r = d.shell("am start -a android.intent.action.VIEW -d market://details?id=$pkg")
            val out = (r.output + r.errorOutput).trim()
            if (r.exitCode == 0 && !out.contains("Error", ignoreCase = true)) {
                AdbResult.Ok(Unit)
            } else {
                AdbResult.Err(out.ifBlank { "No se pudo abrir Play Store (¿la tele tiene Play Store?)." })
            }
        } catch (e: Throwable) {
            AdbResult.Err(describe(e, host, 5555))
        }
    }

    /** ¿Está activado el servicio "cerrar avisos" del launcher SG Alfa? */
    suspend fun isAutoClickEnabled(): Boolean = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext false
        runCatching {
            d.shell("settings get secure enabled_accessibility_services").output.contains(AUTOCLICK_COMPONENT)
        }.getOrDefault(false)
    }

    /** Activa o desactiva el servicio "cerrar avisos", sin tocar otros servicios de accesibilidad. */
    suspend fun setAutoClickEnabled(enable: Boolean): AdbResult<Unit> = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
        try {
            val current = d.shell("settings get secure enabled_accessibility_services").output.trim()
            val list = current.takeIf { it.isNotEmpty() && it != "null" }
                ?.split(':')?.filter { it.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            list.remove(AUTOCLICK_COMPONENT)
            if (enable) list.add(AUTOCLICK_COMPONENT)
            if (list.isEmpty()) {
                d.shell("settings delete secure enabled_accessibility_services")
                d.shell("settings put secure accessibility_enabled 0")
            } else {
                d.shell("settings put secure enabled_accessibility_services '${list.joinToString(":")}'")
                d.shell("settings put secure accessibility_enabled 1")
            }
            val now = d.shell("settings get secure enabled_accessibility_services").output
            if (now.contains(AUTOCLICK_COMPONENT) == enable) AdbResult.Ok(Unit)
            else AdbResult.Err("La tele no aceptó el cambio.")
        } catch (e: Throwable) {
            AdbResult.Err(describe(e, host, 5555))
        }
    }

    /** Envía tus reglas al launcher de la tele. Devuelve cuántas reglas leyó. */
    suspend fun pushAutoClickRules(text: String): AdbResult<Int> = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
        try {
            val encoded = java.util.Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
            val r = d.shell(
                "am broadcast -a tv.clean.home.SET_AUTOCLICK_RULES -n tv.clean.home/.RulesReceiver --es rules '$encoded'",
            )
            val count = Regex("ok (\\d+)").find(r.output)?.groupValues?.get(1)?.toIntOrNull()
            if (count != null) AdbResult.Ok(count)
            else AdbResult.Err((r.output + r.errorOutput).trim().ifBlank { "La tele no confirmó las reglas." })
        } catch (e: Throwable) {
            AdbResult.Err(describe(e, host, 5555))
        }
    }

    /** Activa bloqueo de anuncios en toda la TV via DNS privado filtrante (sin root ni app). */
    suspend fun setPrivateDns(hostname: String): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
            try {
                d.shell("settings put global private_dns_mode hostname")
                val r = d.shell("settings put global private_dns_specifier $hostname")
                if (r.exitCode == 0) AdbResult.Ok(Unit)
                else AdbResult.Err((r.errorOutput).ifBlank { "No se pudo fijar el DNS privado" }.trim())
            } catch (e: Throwable) {
                AdbResult.Err(describe(e, host, 5555))
            }
        }

    /** Desactiva el DNS privado (quita el bloqueo de anuncios por DNS). */
    suspend fun clearPrivateDns(): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
            try {
                val r = d.shell("settings put global private_dns_mode off")
                if (r.exitCode == 0) AdbResult.Ok(Unit)
                else AdbResult.Err((r.errorOutput).ifBlank { "No se pudo desactivar el DNS privado" }.trim())
            } catch (e: Throwable) {
                AdbResult.Err(describe(e, host, 5555))
            }
        }

    /** Devuelve el hostname de DNS privado activo, o null si está apagado. */
    suspend fun currentPrivateDns(): String? = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext null
        runCatching {
            val mode = d.shell("settings get global private_dns_mode").output.trim()
            if (!mode.equals("hostname", ignoreCase = true)) return@runCatching null
            d.shell("settings get global private_dns_specifier").output.trim()
                .takeIf { it.isNotBlank() && it != "null" }
        }.getOrNull()
    }

    private suspend fun <T> shellLines(
        command: String,
        transform: (List<String>) -> T,
    ): AdbResult<T> = withContext(Dispatchers.IO) {
        val d = dadb ?: return@withContext AdbResult.Err("Sin conexión.")
        try {
            val r = d.shell(command)
            if (r.exitCode != 0) {
                return@withContext AdbResult.Err(
                    r.errorOutput.ifBlank { "Comando falló (código ${r.exitCode})" }.trim(),
                )
            }
            AdbResult.Ok(transform(r.output.lineSequence().toList()))
        } catch (e: Throwable) {
            AdbResult.Err(describe(e, host, 5555))
        }
    }

    fun close() {
        runCatching { dadb?.close() }
        dadb = null
        host = null
    }

    private fun describe(e: Throwable, host: String?, port: Int): String {
        val msg = e.message ?: ""
        return when {
            e is java.net.ConnectException ->
                "No se pudo conectar a $host:$port. ¿Está encendida la TV y activada la depuración por red?"
            e is java.net.SocketTimeoutException ->
                "Tiempo de espera agotado hacia $host:$port."
            e is java.net.UnknownHostException ->
                "Dirección no válida: $host."
            msg.contains("auth", ignoreCase = true) || msg.contains("rejected", ignoreCase = true) ->
                "La TV rechazó la autenticación. Acepta \"Permitir depuración\" en la pantalla."
            else -> msg.ifBlank { e.javaClass.simpleName }
        }
    }
}
