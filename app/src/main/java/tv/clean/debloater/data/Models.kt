package tv.clean.debloater.data

import kotlinx.serialization.Serializable

/** manifest.json en assets/lists. */
@Serializable
data class Manifest(
    val source: String = "",
    val profiles: List<ProfileMeta> = emptyList(),
)

@Serializable
data class ProfileMeta(
    val id: String,
    val name: String,
    val brand: String = "",
    val file: String,
)

/** Un perfil de debloat ya parseado: paquete -> razón (comentario de la lista, si hay). */
data class DebloatProfile(
    val id: String,
    val name: String,
    val brand: String,
    val entries: Map<String, String?>,
)

/** Entrada de la base de datos de UAD-ng (uad_lists.json). */
@Serializable
data class UadEntry(
    val list: String = "",
    val description: String = "",
    val removal: String = "",
    val labels: List<String> = emptyList(),
)

/** Fila mostrada en la UI para cada paquete instalado en la TV. */
data class AppRow(
    val packageName: String,
    val reason: String?,
    val inSelectedProfile: Boolean,
    val inAnyList: Boolean,
    val description: String? = null,
    /** Recommended / Advanced / Expert / Unsafe (de UAD-ng), o null si se desconoce. */
    val risk: String? = null,
    /** true si parece telemetría/rastreo/anuncios. */
    val isTelemetry: Boolean = false,
)

/** Registro de respaldo: qué se quitó y cuándo, para poder restaurar. */
@Serializable
data class BackupRecord(
    val host: String,
    val model: String,
    val timestamp: Long,
    val removed: List<String>,
    /** "uninstall" o "disable": cómo revertir. */
    val action: String = "uninstall",
)
