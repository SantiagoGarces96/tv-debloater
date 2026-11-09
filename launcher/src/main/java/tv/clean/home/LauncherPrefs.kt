package tv.clean.home

import android.content.Context

/** Tema/configuración del launcher, guardado en el dispositivo. */
data class LauncherConfig(
    val accent: Long = 0xFF34D399, // esmeralda
    val columns: Int = 0,          // 0 = automático
    val favorites: Set<String> = emptySet(), // paquetes fijados
    val wallpaper: String = "",    // "" negro, id de degradado, o "custom" (imagen del usuario)
)

/** Fondo original integrado (degradado). Sin imágenes con derechos de autor. */
data class Wallpaper(val id: String, val name: String, val colors: List<Long>)

class LauncherPrefs(context: Context) {

    private val sp = context.getSharedPreferences("clean_home", Context.MODE_PRIVATE)

    fun load(): LauncherConfig = LauncherConfig(
        accent = sp.getLong(KEY_ACCENT, 0xFF34D399),
        columns = sp.getInt(KEY_COLUMNS, 0),
        favorites = sp.getStringSet(KEY_FAV, emptySet())?.toSet() ?: emptySet(),
        wallpaper = sp.getString(KEY_WALL, "") ?: "",
    )

    fun save(config: LauncherConfig) {
        sp.edit()
            .putLong(KEY_ACCENT, config.accent)
            .putInt(KEY_COLUMNS, config.columns)
            .putStringSet(KEY_FAV, config.favorites)
            .putString(KEY_WALL, config.wallpaper)
            .apply()
    }

    companion object {
        private const val KEY_ACCENT = "accent"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_FAV = "favorites"
        private const val KEY_WALL = "wallpaper"

        /** Fondos originales integrados (degradados). */
        val WALLPAPERS: List<Wallpaper> = listOf(
            Wallpaper("", "Negro", listOf(0xFF0B0C0F, 0xFF0B0C0F)),
            Wallpaper("aurora", "Aurora", listOf(0xFF0B0C0F, 0xFF0E3B2E, 0xFF0B0C0F)),
            Wallpaper("atardecer", "Atardecer", listOf(0xFF2A0A1E, 0xFF3A1A2E, 0xFF0B0C0F)),
            Wallpaper("oceano", "Océano", listOf(0xFF061826, 0xFF0A2A3A, 0xFF0B0C0F)),
            Wallpaper("uva", "Uva", listOf(0xFF1A0A2E, 0xFF2A1A3E, 0xFF0B0C0F)),
        )

        /** Paleta de acentos disponible (nombre -> color ARGB). */
        val ACCENTS: List<Pair<String, Long>> = listOf(
            "Esmeralda" to 0xFF34D399,
            "Azul" to 0xFF60A5FA,
            "Violeta" to 0xFFA78BFA,
            "Naranja" to 0xFFFB923C,
            "Rosa" to 0xFFF472B6,
            "Blanco" to 0xFFF4F5F7,
        )

        /** Opciones de columnas (etiqueta -> valor; 0 = automático). */
        val COLUMN_OPTIONS: List<Pair<String, Int>> = listOf(
            "Automático" to 0,
            "4" to 4,
            "5" to 5,
            "6" to 6,
        )
    }
}
