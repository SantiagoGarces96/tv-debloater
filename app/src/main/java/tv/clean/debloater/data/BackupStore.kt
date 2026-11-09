package tv.clean.debloater.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** Guarda y lee respaldos de lo que se quitó, en filesDir/backups/*.json. */
class BackupStore(context: Context) {

    private val dir = File(context.filesDir, "backups").apply { mkdirs() }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun save(record: BackupRecord): File = withContext(Dispatchers.IO) {
        val file = File(dir, "backup-${record.timestamp}.json")
        file.writeText(json.encodeToString(BackupRecord.serializer(), record))
        file
    }

    suspend fun list(): List<BackupRecord> = withContext(Dispatchers.IO) {
        (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .mapNotNull { f ->
                runCatching {
                    json.decodeFromString(BackupRecord.serializer(), f.readText())
                }.getOrNull()
            }
    }

    suspend fun latestForHost(host: String): BackupRecord? =
        list().firstOrNull { it.host == host }
}
