package tv.clean.home

import android.content.Context

/** Tema/configuración del launcher, guardado en el dispositivo. */
data class LauncherConfig(
    val accent: Long = 0xFF34D399, // esmeralda
    val columns: Int = 0,          // 0 = automático
    val favorites: Set<String> = emptySet(), // paquetes fijados
)

class LauncherPrefs(context: Context) {

    private val sp = context.getSharedPreferences("clean_home", Context.MODE_PRIVATE)

    fun load(): LauncherConfig = LauncherConfig(
        accent = sp.getLong(KEY_ACCENT, 0xFF34D399),
        columns = sp.getInt(KEY_COLUMNS, 0),
        favorites = sp.getStringSet(KEY_FAV, emptySet())?.toSet() ?: emptySet(),
    )

    fun save(config: LauncherConfig) {
        sp.edit()
            .putLong(KEY_ACCENT, config.accent)
            .putInt(KEY_COLUMNS, config.columns)
            .putStringSet(KEY_FAV, config.favorites)
            .apply()
    }

    companion object {
        private const val KEY_ACCENT = "accent"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_FAV = "favorites"

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
