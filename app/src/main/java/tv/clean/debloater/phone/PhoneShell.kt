package tv.clean.debloater.phone

import kotlin.system.exitProcess

/** Corre dentro de Shizuku (permisos de ADB, sin root). Solo quita o devuelve apps de la lista. */
class PhoneShell : IPhoneShell.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(cmd: String?): String {
        if (cmd == null || !PhoneCatalog.isAllowedCommand(cmd)) return "Orden no permitida"
        return try {
            val p = ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            out.trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
