package tv.clean.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import tv.clean.automation.Macros

/**
 * Recibe tus automatizaciones desde la app del teléfono (por ADB).
 * Protegido con el permiso DUMP en el manifiesto: solo ADB puede enviarlo, ninguna otra app de la tele.
 */
class RulesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val encoded = intent.getStringExtra(EXTRA_MACROS) ?: return
        val json = runCatching { String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8) }.getOrNull() ?: return
        if (json.length > MAX_CHARS) return
        val macros = Macros.decodeOrNull(json) ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MACROS, Macros.encode(macros))
            .remove(KEY_LEGACY)
            .apply()
        runCatching {
            resultCode = Activity.RESULT_OK
            resultData = "ok ${macros.size}"
        }
    }

    companion object {
        const val ACTION = "tv.clean.home.SET_AUTOCLICK_RULES"
        const val EXTRA_MACROS = "macros"
        const val PREFS = "autoclick"
        const val KEY_MACROS = "macros"

        /** Reglas antiguas en texto (versión anterior); se migran solas. */
        const val KEY_LEGACY = "rules"

        private const val MAX_CHARS = 100_000
    }
}
