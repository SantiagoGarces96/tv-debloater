package tv.clean.debloater.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga APKs para instalarlos en la tele.
 *
 * Acepta:
 *  - una URL (https://…/app.apk),
 *  - un código corto de AFTVnews (solo números): se resuelve como https://aftv.news/<código>,
 *  - un repo de GitHub: busca el APK de la última release.
 *
 * Siempre comprueba que lo descargado sea un APK real (firma ZIP "PK"), no una página web.
 */
object ApkDownloader {

    private const val MAX_BYTES = 300L * 1024 * 1024
    private const val MAX_REDIRECTS = 10
    private const val UA = "SG-Alfa/1.0 (Android)"

    sealed interface Result {
        data class Ok(val file: File, val finalUrl: String, val bytes: Long) : Result
        data class Err(val message: String) : Result
    }

    /** Convierte lo que escribió el usuario en una URL. */
    fun toUrl(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        if (text.all { it.isDigit() }) return "https://aftv.news/$text"
        val withScheme = if (text.startsWith("http://") || text.startsWith("https://")) text else "https://$text"
        return runCatching { URL(withScheme).toString() }.getOrNull()
    }

    fun isCode(input: String): Boolean = input.trim().let { it.isNotEmpty() && it.all(Char::isDigit) }

    /** URL de descarga del APK de la última release de un repo de GitHub ("dueño/repo"). */
    suspend fun latestGithubApk(repo: String, assetPattern: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 15_000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val assets = Json.parseToJsonElement(body).jsonObject["assets"]?.jsonArray ?: return@runCatching null
            val apks = assets.map { it.jsonObject }
                .map { (it["name"]?.jsonPrimitive?.content ?: "") to (it["browser_download_url"]?.jsonPrimitive?.content ?: "") }
                .filter { (name, url) -> name.endsWith(".apk", ignoreCase = true) && url.isNotEmpty() }
            val regex = assetPattern.takeIf { it.isNotBlank() }?.let { Regex(it, RegexOption.IGNORE_CASE) }
            (apks.firstOrNull { regex != null && regex.containsMatchIn(it.first) } ?: apks.firstOrNull())?.second
        }.getOrNull()
    }

    /** Descarga [url] en [dest], siguiendo redirecciones, y verifica que sea un APK. */
    suspend fun download(url: String, dest: File, onLog: (String) -> Unit): Result = withContext(Dispatchers.IO) {
        try {
            var current = url
            var conn: HttpURLConnection? = null
            for (hop in 0..MAX_REDIRECTS) {
                val c = (URL(current).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", UA)
                    connectTimeout = 15_000
                    readTimeout = 30_000
                }
                val code = c.responseCode
                if (code in 300..399) {
                    val location = c.getHeaderField("Location")
                    c.disconnect()
                    if (location.isNullOrBlank()) return@withContext Result.Err("Redirección sin destino.")
                    current = URL(URL(current), location).toString()
                    onLog("→ ${URL(current).host}")
                    continue
                }
                if (code !in 200..299) {
                    c.disconnect()
                    return@withContext Result.Err("El servidor respondió $code.")
                }
                conn = c
                break
            }
            val c = conn ?: return@withContext Result.Err("Demasiadas redirecciones.")
            val type = c.contentType.orEmpty()
            val length = c.contentLengthLong
            if (length > MAX_BYTES) {
                c.disconnect()
                return@withContext Result.Err("El archivo es demasiado grande (${length / 1_048_576} MB).")
            }
            onLog("Descargando de ${URL(current).host}…")
            var total = 0L
            c.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var lastReport = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) return@withContext Result.Err("El archivo supera 300 MB.")
                        output.write(buf, 0, n)
                        if (total - lastReport >= 5L * 1_048_576) {
                            onLog("  ${total / 1_048_576} MB…")
                            lastReport = total
                        }
                    }
                }
            }
            c.disconnect()
            if (!looksLikeApk(dest)) {
                dest.delete()
                val hint = if (type.contains("html", true)) " (es una página web)" else ""
                return@withContext Result.Err("Lo descargado no es un APK$hint. Destino: $current")
            }
            Result.Ok(dest, current, total)
        } catch (e: Throwable) {
            dest.delete()
            Result.Err(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Un APK es un ZIP: empieza por "PK\u0003\u0004". */
    fun looksLikeApk(file: File): Boolean = runCatching {
        file.inputStream().use { s ->
            val head = ByteArray(4)
            s.read(head) == 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() &&
                head[2] == 3.toByte() && head[3] == 4.toByte()
        }
    }.getOrDefault(false)
}
