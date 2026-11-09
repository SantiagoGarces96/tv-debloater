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

    private companion object {
        const val KEY_LAST_HOST = "last_host"
    }
}
