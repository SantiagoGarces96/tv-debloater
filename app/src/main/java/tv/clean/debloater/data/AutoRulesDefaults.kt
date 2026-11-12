package tv.clean.debloater.data

/** Reglas por defecto de "cerrar avisos" (las mismas que trae el launcher de la tele). */
object AutoRulesDefaults {
    val TEXT: String = """
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
}
