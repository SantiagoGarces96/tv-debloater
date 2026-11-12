package tv.clean.home

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * "Cerrar avisos automáticamente": cuando en pantalla aparece un botón que coincide con
 * tus reglas (p. ej. «Continuar usando», «Saltar anuncio»), lo pulsa solo.
 *
 * No guarda ni envía nada de lo que ve: solo compara textos de botones con tus reglas.
 * Limitación: solo funciona en apps que muestran sus botones de forma accesible.
 */
class AutoClickService : AccessibilityService() {

    @Volatile private var rules: List<AutoClickRule> = emptyList()
    private val lastClick = HashMap<String, Long>()

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> loadRules() }

    private fun prefs() = getSharedPreferences(RulesReceiver.PREFS, MODE_PRIVATE)

    private fun loadRules() {
        rules = AutoClickRules.parse(prefs().getString(RulesReceiver.KEY, null) ?: AutoClickRules.DEFAULT_TEXT)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        loadRules()
        prefs().registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val screenText = StringBuilder()
        val candidates = ArrayList<AccessibilityNodeInfo>()
        collect(root, screenText, candidates, depth = 0)
        val screen = screenText.toString()

        for (node in candidates) {
            val label = (node.text ?: node.contentDescription)?.toString() ?: continue
            val rule = AutoClickRules.match(label, screen, rules) ?: continue
            val now = SystemClock.uptimeMillis()
            if (now - (lastClick[rule.click] ?: 0L) < COOLDOWN_MS) continue
            if (click(node)) {
                lastClick[rule.click] = now
                break
            }
        }
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        screen: StringBuilder,
        out: MutableList<AccessibilityNodeInfo>,
        depth: Int,
    ) {
        if (depth > MAX_DEPTH || out.size > MAX_NODES) return
        val text = node.text
        val desc = node.contentDescription
        if (text != null) screen.append(text).append(' ')
        if (desc != null) screen.append(desc).append(' ')
        if (text != null || desc != null) out.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collect(it, screen, out, depth + 1) }
        }
    }

    /** Pulsa el nodo o, si no es pulsable, el contenedor pulsable más cercano. */
    private fun click(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var hops = 0
        while (current != null && hops <= 4) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            hops++
        }
        return false
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        runCatching { prefs().unregisterOnSharedPreferenceChangeListener(prefsListener) }
        super.onDestroy()
    }

    private companion object {
        const val COOLDOWN_MS = 2_000L
        const val MAX_DEPTH = 40
        const val MAX_NODES = 600
    }
}
