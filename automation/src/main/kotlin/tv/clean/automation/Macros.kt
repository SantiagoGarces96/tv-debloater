package tv.clean.automation

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/** CUANDO: qué tiene que aparecer en pantalla. */
@Serializable
enum class Trigger {
    /** Aparece un botón con ese texto. */
    BUTTON,

    /** Aparece ese texto en cualquier parte de la pantalla. */
    TEXT,
}

/** ENTONCES: qué hace la tele. */
@Serializable
enum class Action {
    /** Pulsar el botón que se encontró (solo con disparador BUTTON). */
    CLICK,

    /** Pulsar Atrás. */
    BACK,

    /** Ir a la pantalla de inicio. */
    HOME,
}

/**
 * Una automatización, al estilo MacroDroid:
 * CUANDO aparezca [text] (botón o texto) — SOLO SI se ve [context] y estamos en [app] —
 * ENTONCES hacer [action], tras [delaySeconds].
 */
@Serializable
data class Macro(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val trigger: Trigger = Trigger.BUTTON,
    val text: String = "",
    val context: List<String> = emptyList(),
    val action: Action = Action.CLICK,
    val app: String = "",
    val delaySeconds: Int = 0,
)

object Macros {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val listSerializer = ListSerializer(Macro.serializer())

    fun encode(list: List<Macro>): String = json.encodeToString(listSerializer, list)

    /** null si el texto no es una lista válida de automatizaciones. */
    fun decodeOrNull(text: String): List<Macro>? =
        runCatching { json.decodeFromString(listSerializer, text) }.getOrNull()

    fun newId(): String = UUID.randomUUID().toString().take(8)

    // ---------- Comparación de textos ----------

    private val nonWord = Regex("[^\\p{L}\\p{N} ]")
    private val spaces = Regex("\\s+")

    /** Minúsculas, sin signos y con espacios simples, para comparar sin fallar por detalles. */
    fun normalize(s: String): String = s.lowercase().replace(nonWord, " ").replace(spaces, " ").trim()

    private fun appMatches(m: Macro, pkg: String?): Boolean = m.app.isBlank() || m.app == pkg

    private fun contextMatches(m: Macro, normalizedScreen: String): Boolean =
        m.context.isEmpty() || m.context.any { c ->
            val n = normalize(c)
            n.isNotEmpty() && normalizedScreen.contains(n)
        }

    /**
     * ¿Este botón dispara la automatización?
     * Solo textos cortos (botones, no párrafos). Con restricción de texto, o si el texto es corto
     * (menos de 6 letras), el botón tiene que coincidir exactamente.
     */
    fun buttonMatches(m: Macro, buttonText: String, screenText: String, pkg: String?): Boolean {
        if (!m.enabled || m.trigger != Trigger.BUTTON || !appMatches(m, pkg)) return false
        val button = normalize(buttonText)
        val wanted = normalize(m.text)
        if (wanted.isEmpty() || button.isEmpty() || button.length > 40) return false
        val hit = if (m.context.isEmpty() && wanted.length >= 6) {
            button == wanted || button.contains(wanted)
        } else {
            button == wanted
        }
        return hit && contextMatches(m, normalize(screenText))
    }

    /** ¿Aparece el texto en pantalla (para disparadores TEXT)? */
    fun textMatches(m: Macro, screenText: String, pkg: String?): Boolean {
        if (!m.enabled || m.trigger != Trigger.TEXT || !appMatches(m, pkg)) return false
        val wanted = normalize(m.text)
        if (wanted.length < 3) return false
        val screen = normalize(screenText)
        return screen.contains(wanted) && contextMatches(m, screen)
    }

    // ---------- Para la pantalla del teléfono ----------

    /** null si está bien; si no, qué hay que corregir. */
    fun validate(m: Macro): String? = when {
        m.name.isBlank() -> "Ponle un nombre."
        normalize(m.text).length < 2 -> "Escribe el texto que tiene que aparecer."
        m.trigger == Trigger.TEXT && normalize(m.text).length < 3 -> "El texto tiene que tener al menos 3 letras."
        m.trigger == Trigger.TEXT && m.action == Action.CLICK ->
            "«Pulsar ese botón» solo sirve cuando el disparador es un botón."
        m.delaySeconds !in 0..30 -> "La espera tiene que estar entre 0 y 30 segundos."
        else -> null
    }

    /** Frase que explica la automatización, p. ej. «Cuando aparezca el botón «Sí»…, pulsarlo al instante». */
    fun describe(m: Macro): String {
        val trigger = when (m.trigger) {
            Trigger.BUTTON -> "Cuando aparezca el botón «${m.text.trim()}»"
            Trigger.TEXT -> "Cuando aparezca en pantalla «${m.text.trim()}»"
        }
        val context = m.context.filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" o ") { "«${it.trim()}»" }
            ?.let { " y también se vea $it" }
            .orEmpty()
        val app = if (m.app.isNotBlank()) " en ${m.app}" else ""
        val action = when (m.action) {
            Action.CLICK -> "pulsarlo"
            Action.BACK -> "pulsar Atrás"
            Action.HOME -> "ir al inicio"
        }
        val delay = if (m.delaySeconds > 0) "tras ${m.delaySeconds} s" else "al instante"
        return "$trigger$context$app, $action $delay."
    }

    /** Automatizaciones listas para usar. También son las que vienen activadas de inicio. */
    fun templates(): List<Macro> = listOf(
        Macro("tpl-continuar", "Seguir usando la app", text = "Continuar usando"),
        Macro("tpl-seguir", "Seguir viendo", text = "Seguir viendo"),
        Macro("tpl-cont-viendo", "Continuar viendo", text = "Continuar viendo"),
        Macro(
            "tpl-si",
            "¿Sigues viendo? → Sí",
            text = "Sí",
            context = listOf("sigues viendo", "continuar viendo", "deseas continuar"),
        ),
        Macro("tpl-saltar", "Saltar anuncio", text = "Saltar anuncio"),
        Macro("tpl-omitir", "Omitir anuncio", text = "Omitir anuncio"),
        Macro("tpl-skip", "Skip ad (inglés)", text = "Skip ad"),
        Macro("tpl-keep", "Keep watching (inglés)", text = "Keep watching"),
        Macro("tpl-continue", "Continue watching (inglés)", text = "Continue watching"),
        Macro(
            "tpl-yes",
            "Still watching? → Yes (inglés)",
            text = "Yes",
            context = listOf("still watching", "continue watching"),
        ),
        // Ventanas emergentes (pop-ups) de anuncios
        Macro("tpl-pop-cerrar-anuncio", "Pop-up: Cerrar anuncio", text = "Cerrar anuncio"),
        Macro("tpl-pop-close-ad", "Pop-up: Close ad (inglés)", text = "Close ad"),
        Macro("tpl-pop-cerrar", "Pop-up de anuncio → Cerrar", text = "Cerrar", context = AD_WORDS),
        Macro("tpl-pop-close", "Pop-up de anuncio → Close (inglés)", text = "Close", context = AD_WORDS),
        Macro(
            "tpl-pop-nogracias",
            "Pop-up de oferta → No, gracias",
            text = "No, gracias",
            context = listOf("oferta", "premium", "suscríbete", "suscribete", "promoción", "promocion"),
        ),
    )

    /** Palabras que delatan un pop-up de publicidad. Igual que en SG Alfa Home (Macros.java). */
    private val AD_WORDS = listOf(
        "anuncio", "publicidad", "patrocinado", "advertisement", "sponsored", "promocion", "promoción",
    )

    /**
     * Convierte las reglas antiguas en texto (una por línea, «botón | contexto, contexto»)
     * en automatizaciones, para no perder lo que ya tenías.
     */
    fun fromLegacyText(text: String): List<Macro> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val parts = line.split('|', limit = 2)
            val button = parts[0].trim()
            if (normalize(button).isEmpty()) {
                null
            } else {
                val context = parts.getOrNull(1)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                Macro(id = newId(), name = button, text = button, context = context)
            }
        }
        .toList()
}
