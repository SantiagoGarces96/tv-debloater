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

/** Fila mostrada en la UI para cada paquete instalado en la TV. */
data class AppRow(
    val packageName: String,
    val reason: String?,
    val inSelectedProfile: Boolean,
    val inAnyList: Boolean,
)

/** Registro de respaldo: qué se quitó y cuándo, para poder restaurar. */
@Serializable
data class BackupRecord(
    val host: String,
    val model: String,
    val timestamp: Long,
    val removed: List<String>,
)
