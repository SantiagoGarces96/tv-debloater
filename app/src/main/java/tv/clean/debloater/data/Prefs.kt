package tv.clean.debloater.data

import android.content.Context

/** Preferencias simples (última TV usada, etc.). */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("clean_tv", Context.MODE_PRIVATE)

    var lastHost: String
        get() = sp.getString(KEY_LAST_HOST, "").orEmpty()
        set(value) {
            sp.edit().putString(KEY_LAST_HOST, value).apply()
        }

    /** Reglas antiguas en texto (versiones anteriores). Solo se leen para migrarlas. */
    val legacyAutoRules: String
        get() = sp.getString(KEY_AUTO_RULES, "").orEmpty()

    /** Tus automatizaciones (JSON). Vacío = todavía no has guardado ninguna. */
    var macrosJson: String
        get() = sp.getString(KEY_MACROS, "").orEmpty()
        set(value) {
            sp.edit().putString(KEY_MACROS, value).remove(KEY_AUTO_RULES).apply()
        }

    private companion object {
        const val KEY_LAST_HOST = "last_host"
        const val KEY_AUTO_RULES = "auto_rules"
        const val KEY_MACROS = "macros"
    }
}
