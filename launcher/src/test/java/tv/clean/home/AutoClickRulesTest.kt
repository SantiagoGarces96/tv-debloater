package tv.clean.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoClickRulesTest {

    private val rules = AutoClickRules.parse(AutoClickRules.DEFAULT_TEXT)

    @Test
    fun parse_readsRulesAndContext() {
        assertTrue(rules.size >= 10)
        val si = rules.first { it.click == "sí" }
        assertTrue(si.context.contains("sigues viendo"))
    }

    @Test
    fun clicks_continueUsingDialog() {
        val screen = "Consejos Ya usaste el app por 4 horas, te recomiendo descansar. Continuar usando"
        assertNotNull(AutoClickRules.match("Continuar usando", screen, rules))
    }

    @Test
    fun clicks_yes_onlyWhenAskingIfStillWatching() {
        assertNotNull(AutoClickRules.match("Sí", "Video en pausa. ¿Sigues viendo?", rules))
        assertNull(AutoClickRules.match("Sí", "¿Eliminar esta lista de reproducción?", rules))
    }

    @Test
    fun clicks_skipAdWithCountdownText() {
        assertNotNull(AutoClickRules.match("Skip Ad ▶", "", rules))
        assertNotNull(AutoClickRules.match("Saltar anuncio", "", rules))
    }

    @Test
    fun ignores_longParagraphs() {
        val paragraph = "Para seguir viendo contenido sin anuncios suscríbete a nuestro plan premium hoy mismo"
        assertNull(AutoClickRules.match(paragraph, paragraph, rules))
    }

    @Test
    fun normalize_ignoresCaseAndPunctuation() {
        assertEquals("sí seguir viendo", AutoClickRules.normalize("¡SÍ, seguir viendo!"))
    }
}
