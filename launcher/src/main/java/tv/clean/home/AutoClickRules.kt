package tv.clean.home

/** Una regla: pulsar el botón con texto [click], solo si en pantalla aparece alguno de [context] (si hay). */
data class AutoClickRule(val click: String, val context: List<String> = emptyList())

/**
 * Reglas de "cerrar avisos automáticamente".
 *
 * Formato (una regla por línea):
 *   Continuar usando
 *   Sí | sigues viendo, continuar viendo
 * Lo que va antes de "|" es el texto del botón a pulsar. Lo que va después (opcional) son
 * textos que deben verse en pantalla para que se pulse. Las líneas con # son comentarios.
 */
object AutoClickRules {

    val DEFAULT_TEXT: String = """
        # Una regla por línea: el texto del botón que quieres que se pulse solo.
        # Opcional: después de | pon textos que deben verse en pantalla (separados por coma).
        Continuar usando
        Seguir viendo
        Continuar viendo
        Sí, seguir viendo
        Continue watching
        Keep watching
        Saltar anuncio
        Saltar anuncios
        Omitir anuncio
        Omitir anuncios
        Skip ad
        Skip ads
        Sí | sigues viendo, continuar viendo, deseas continuar
        Yes | still watching, continue watching
    """.trimIndent()

    private val nonWord = Regex("[^\\p{L}\\p{N} ]")
    private val spaces = Regex("\\s+")

    /** Minúsculas, sin signos y con espacios simples, para comparar textos sin fallar por detalles. */
    fun normalize(s: String): String =
        s.lowercase().replace(nonWord, " ").replace(spaces, " ").trim()

    fun parse(text: String): List<AutoClickRule> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val parts = line.split('|', limit = 2)
            val click = normalize(parts[0])
            if (click.isEmpty()) {
                null
            } else {
                val context = parts.getOrNull(1)
                    ?.split(',')
                    ?.map { normalize(it) }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()
                AutoClickRule(click, context)
            }
        }
        .toList()

    /**
     * ¿Hay que pulsar un botón con este [buttonText], viendo [screenText] en pantalla?
     * Devuelve la regla que coincide, o null.
     *
     * Cuidado incorporado: solo textos cortos (botones, no párrafos); las reglas con contexto
     * exigen coincidencia exacta del botón; las reglas cortas (menos de 6 letras) también.
     */
    fun match(buttonText: String, screenText: String, rules: List<AutoClickRule>): AutoClickRule? {
        val button = normalize(buttonText)
        if (button.isEmpty() || button.length > 40) return null
        val screen = normalize(screenText)
        return rules.firstOrNull { rule ->
            val hit = if (rule.context.isEmpty() && rule.click.length >= 6) {
                button == rule.click || button.contains(rule.click)
            } else {
                button == rule.click
            }
            hit && (rule.context.isEmpty() || rule.context.any { screen.contains(it) })
        }
    }
}
