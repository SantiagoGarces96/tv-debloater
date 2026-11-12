package tv.clean.debloater.data

import kotlinx.serialization.Serializable

/**
 * Una app del catálogo "Descargar e instalar".
 *
 * type:
 *  - "playstore": abre la ficha oficial en Play Store de la tele (pkg).
 *  - "github":    descarga el APK de la última release oficial (repo "dueño/repo", assetPattern opcional).
 *  - "url":       descarga un APK desde una URL directa (url).
 *  - "code":      código corto de AFTVnews (code), verificado antes de añadirlo.
 */
@Serializable
data class CatalogApp(
    val name: String,
    val description: String = "",
    val type: String,
    val pkg: String = "",
    val repo: String = "",
    val assetPattern: String = "",
    val url: String = "",
    val code: String = "",
)

@Serializable
data class AppCatalog(
    val apps: List<CatalogApp> = emptyList(),
)
