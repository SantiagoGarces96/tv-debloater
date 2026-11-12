package tv.clean.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64

/**
 * Recibe tus reglas de "cerrar avisos" desde la app del teléfono (por ADB).
 * Protegido con el permiso DUMP en el manifiesto: solo ADB puede enviarlo, ninguna otra app de la tele.
 */
class RulesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val encoded = intent.getStringExtra(EXTRA_RULES) ?: return
        val text = runCatching { String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8) }.getOrNull() ?: return
        if (text.length > MAX_CHARS) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, text).apply()
        runCatching {
            resultCode = Activity.RESULT_OK
            resultData = "ok ${AutoClickRules.parse(text).size}"
        }
    }

    companion object {
        const val ACTION = "tv.clean.home.SET_AUTOCLICK_RULES"
        const val EXTRA_RULES = "rules"
        const val PREFS = "autoclick"
        const val KEY = "rules"
        private const val MAX_CHARS = 20_000
    }
}
