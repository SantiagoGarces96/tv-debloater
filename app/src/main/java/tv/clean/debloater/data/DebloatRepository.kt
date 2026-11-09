package tv.clean.debloater.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Carga y parsea las listas de debloat empaquetadas en assets/lists.
 * Las listas provienen de CleanYourTV (MIT); ver NOTICE.
 */
class DebloatRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: List<DebloatProfile>? = null

    suspend fun profiles(): List<DebloatProfile> = withContext(Dispatchers.IO) {
        cached ?: loadProfiles().also { cached = it }
    }

    /** Catálogo combinado de todas las listas: paquete -> primera razón conocida. */
    suspend fun mergedCatalog(): Map<String, String?> = withContext(Dispatchers.IO) {
        val catalog = LinkedHashMap<String, String?>()
        for (profile in profiles()) {
            for ((pkg, reason) in profile.entries) {
                val existing = catalog[pkg]
                if (existing == null && reason != null) {
                    catalog[pkg] = reason
                } else if (pkg !in catalog) {
                    catalog[pkg] = reason
                }
            }
        }
        catalog
    }

    private fun loadProfiles(): List<DebloatProfile> {
        val assets = context.assets
        val manifestText = assets.open("lists/manifest.json").bufferedReader().use { it.readText() }
        val manifest = json.decodeFromString<Manifest>(manifestText)
        return manifest.profiles.mapNotNull { meta ->
            runCatching {
                val text = assets.open("lists/${meta.file}").bufferedReader().use { it.readText() }
                DebloatProfile(
                    id = meta.id,
                    name = meta.name,
                    brand = meta.brand,
                    entries = parseList(text),
                )
            }.getOrNull()
        }
    }

    companion object {
        /**
         * Parsea un debloat.txt: una línea por paquete, con comentario opcional tras '#'.
         * Las líneas que empiezan con '#' o vacías se ignoran.
         */
        fun parseList(text: String): Map<String, String?> {
            val out = LinkedHashMap<String, String?>()
            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val hash = line.indexOf('#')
                val pkg = (if (hash >= 0) line.substring(0, hash) else line).trim()
                if (pkg.isEmpty()) continue
                val reason = if (hash >= 0) line.substring(hash + 1).trim().ifBlank { null } else null
                out[pkg] = reason
            }
            return out
        }
    }
}
