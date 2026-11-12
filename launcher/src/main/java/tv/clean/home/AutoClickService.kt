package tv.clean.home

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import tv.clean.automation.Action
import tv.clean.automation.Macro
import tv.clean.automation.Macros
import tv.clean.automation.Trigger

/**
 * Automatizaciones en la tele (estilo MacroDroid): CUANDO aparece un botón o un texto,
 * SOLO SI se cumplen las restricciones, ENTONCES pulsa el botón / Atrás / Inicio.
 *
 * No guarda ni envía nada de lo que ve: solo compara textos con tus automatizaciones.
 * Limitación: solo funciona en apps que muestran su interfaz de forma accesible.
 */
class AutoClickService : AccessibilityService() {

    @Volatile private var macros: List<Macro> = emptyList()
    private val lastRun = HashMap<String, Long>()
    private val pending = HashSet<String>()
    private val handler = Handler(Looper.getMainLooper())

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> loadMacros() }

    private fun prefs() = getSharedPreferences(RulesReceiver.PREFS, MODE_PRIVATE)

    /** Automatizaciones guardadas; si no hay, migra las reglas viejas; si tampoco, usa las plantillas. */
    private fun loadMacros() {
        val p = prefs()
        macros = p.getString(RulesReceiver.KEY_MACROS, null)?.let { Macros.decodeOrNull(it) }
            ?: p.getString(RulesReceiver.KEY_LEGACY, null)?.let { Macros.fromLegacyText(it) }
            ?: Macros.templates()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        loadMacros()
        prefs().registerOnSharedPreferenceChangeListener(prefsListener)
    }

    /** Lo que hay en pantalla ahora: app, todo el texto y los elementos con texto. */
    private class Snapshot(
        val pkg: String?,
        val screen: String,
        val nodes: List<AccessibilityNodeInfo>,
    )

    private fun snapshot(): Snapshot? {
        val root = rootInActiveWindow ?: return null
        val screen = StringBuilder()
        val nodes = ArrayList<AccessibilityNodeInfo>()
        collect(root, screen, nodes, 0)
        return Snapshot(root.packageName?.toString(), screen.toString(), nodes)
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

    private fun label(node: AccessibilityNodeInfo): String? =
        (node.text ?: node.contentDescription)?.toString()

    /** Busca si [m] se cumple ahora. Devuelve (se cumple, botón encontrado si aplica). */
    private fun evaluate(m: Macro, snap: Snapshot): Pair<Boolean, AccessibilityNodeInfo?> = when (m.trigger) {
        Trigger.BUTTON -> {
            val node = snap.nodes.firstOrNull { n ->
                label(n)?.let { Macros.buttonMatches(m, it, snap.screen, snap.pkg) } == true
            }
            (node != null) to node
        }
        Trigger.TEXT -> Macros.textMatches(m, snap.screen, snap.pkg) to null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val snap = snapshot() ?: return
        val now = SystemClock.uptimeMillis()
        for (m in macros) {
            if (!m.enabled || m.id in pending) continue
            if (now - (lastRun[m.id] ?: 0L) < cooldown(m)) continue
            val (hit, node) = evaluate(m, snap)
            if (!hit) continue
            lastRun[m.id] = now
            if (m.delaySeconds > 0) {
                pending += m.id
                handler.postDelayed({ runAfterDelay(m) }, m.delaySeconds * 1000L)
            } else {
                perform(m, node)
            }
            break // una acción por evento, para no encadenar cosas raras
        }
    }

    /** Tras la espera, vuelve a mirar: solo actúa si el aviso sigue ahí. */
    private fun runAfterDelay(m: Macro) {
        pending -= m.id
        val snap = snapshot() ?: return
        val (hit, node) = evaluate(m, snap)
        if (hit) perform(m, node)
    }

    private fun perform(m: Macro, node: AccessibilityNodeInfo?) {
        when (m.action) {
            Action.CLICK -> node?.let { click(it) }
            Action.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            Action.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /** Pulsar es inofensivo cada 2 s; Atrás/Inicio esperan más para no sacarte de la app en bucle. */
    private fun cooldown(m: Macro): Long = if (m.action == Action.CLICK) 2_000L else 6_000L

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
        handler.removeCallbacksAndMessages(null)
        runCatching { prefs().unregisterOnSharedPreferenceChangeListener(prefsListener) }
        super.onDestroy()
    }

    private companion object {
        const val MAX_DEPTH = 40
        const val MAX_NODES = 600
    }
}
