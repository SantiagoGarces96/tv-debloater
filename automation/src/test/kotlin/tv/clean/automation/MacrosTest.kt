package tv.clean.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MacrosTest {

    private val templates = Macros.templates()
    private fun byId(id: String) = templates.first { it.id == id }

    @Test
    fun button_continueUsingDialog() {
        val screen = "Consejos Ya usaste el app por 4 horas, te recomiendo descansar. Continuar usando"
        assertTrue(Macros.buttonMatches(byId("tpl-continuar"), "Continuar usando", screen, "com.any"))
    }

    @Test
    fun button_yesOnlyWhenAskingIfStillWatching() {
        val si = byId("tpl-si")
        assertTrue(Macros.buttonMatches(si, "Sí", "Video en pausa. ¿Sigues viendo?", null))
        assertFalse(Macros.buttonMatches(si, "Sí", "¿Eliminar esta lista de reproducción?", null))
    }

    @Test
    fun button_skipAdWithCountdown_andIgnoresParagraphs() {
        assertTrue(Macros.buttonMatches(byId("tpl-skip"), "Skip Ad ▶", "", null))
        val paragraph = "Para seguir viendo contenido sin anuncios suscríbete a nuestro plan premium hoy mismo"
        assertFalse(Macros.buttonMatches(byId("tpl-seguir"), paragraph, paragraph, null))
    }

    @Test
    fun popup_closeOnlyWhenItIsAnAd() {
        val cerrar = byId("tpl-pop-cerrar")
        assertTrue(Macros.buttonMatches(cerrar, "Cerrar", "Publicidad · Descarga ya nuestro juego", null))
        // Un «Cerrar» normal (sin anuncio en pantalla) no se toca:
        assertFalse(Macros.buttonMatches(cerrar, "Cerrar", "¿Cerrar sesión de tu cuenta?", null))
        assertTrue(Macros.buttonMatches(byId("tpl-pop-nogracias"), "No, gracias", "Oferta: 3 meses Premium", null))
    }

    @Test
    fun disabledMacro_neverFires() {
        val off = byId("tpl-continuar").copy(enabled = false)
        assertFalse(Macros.buttonMatches(off, "Continuar usando", "", null))
    }

    @Test
    fun appRestriction() {
        val m = byId("tpl-continuar").copy(app = "com.video.app")
        assertTrue(Macros.buttonMatches(m, "Continuar usando", "", "com.video.app"))
        assertFalse(Macros.buttonMatches(m, "Continuar usando", "", "com.otra.app"))
    }

    @Test
    fun textTrigger_backAction() {
        val m = Macro("x", "Cerrar publicidad", trigger = Trigger.TEXT, text = "Publicidad", action = Action.BACK)
        assertTrue(Macros.textMatches(m, "Esto es PUBLICIDAD patrocinada", null))
        assertFalse(Macros.textMatches(m, "Nada que ver aquí", null))
        // Un disparador de texto no responde como botón:
        assertFalse(Macros.buttonMatches(m, "Publicidad", "Publicidad", null))
    }

    @Test
    fun validate_catchesMistakes() {
        assertNull(Macros.validate(byId("tpl-continuar")))
        assertNotNull(Macros.validate(Macro("a", "", text = "Hola")))
        assertNotNull(Macros.validate(Macro("a", "Nombre", text = "")))
        assertNotNull(Macros.validate(Macro("a", "Nombre", trigger = Trigger.TEXT, text = "Publicidad", action = Action.CLICK)))
        assertNotNull(Macros.validate(Macro("a", "Nombre", text = "Ok", delaySeconds = 99)))
    }

    @Test
    fun describe_readsLikeASentence() {
        val m = Macro(
            "a", "x", text = "Sí", context = listOf("sigues viendo"), app = "com.app", delaySeconds = 3,
        )
        assertEquals(
            "Cuando aparezca el botón «Sí» y también se vea «sigues viendo» en com.app, pulsarlo tras 3 s.",
            Macros.describe(m),
        )
    }

    @Test
    fun encodeDecode_roundTrip_andRejectsGarbage() {
        val list = templates.take(3)
        assertEquals(list, Macros.decodeOrNull(Macros.encode(list)))
        assertEquals(emptyList<Macro>(), Macros.decodeOrNull("[]"))
        assertNull(Macros.decodeOrNull("esto no es json"))
    }

    @Test
    fun legacyText_isMigrated() {
        val migrated = Macros.fromLegacyText("# comentario\nContinuar usando\nSí | sigues viendo, deseas continuar\n")
        assertEquals(2, migrated.size)
        assertEquals("Continuar usando", migrated[0].text)
        assertEquals(listOf("sigues viendo", "deseas continuar"), migrated[1].context)
    }
}
