package tv.clean.debloater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.clean.debloater.data.AppGroup
import tv.clean.debloater.data.AppRow
import tv.clean.debloater.data.Categorizer
import tv.clean.debloater.data.DebloatRepository
import tv.clean.debloater.data.Telemetry
import tv.clean.debloater.data.UadEntry

class LogicTest {

    @Test
    fun parseList_stripsCommentsAndInlineReasons() {
        val text = """
            # cabecera
            com.foo.bar
            com.baz.qux   # motivo en linea

            com.only.pkg
        """.trimIndent()
        val map = DebloatRepository.parseList(text)
        assertEquals(3, map.size)
        assertTrue(map.containsKey("com.foo.bar"))
        assertEquals(null, map["com.foo.bar"])
        assertEquals("motivo en linea", map["com.baz.qux"])
    }

    @Test
    fun telemetry_detectsByNameAndText() {
        assertTrue(Telemetry.isTelemetry("com.amazon.hybridadidservice", null, null, emptyList()))
        assertTrue(Telemetry.isTelemetry("com.x.y", "rastreo de uso", null, emptyList()))
        assertTrue(Telemetry.isTelemetry("com.x.y", null, "advertising id service", emptyList()))
        assertFalse(Telemetry.isTelemetry("com.netflix.ninja", null, null, emptyList()))
    }

    @Test
    fun categorizer_classifiesGroups() {
        assertEquals(AppGroup.ADS, Categorizer.of("com.x.adservice", null, null, null, false))
        assertEquals(AppGroup.TRACKING, Categorizer.of("com.x.y", null, null, null, true))
        assertEquals(AppGroup.BRAND, Categorizer.of("com.x.y", null, null, UadEntry(list = "Oem"), false))
        assertEquals(AppGroup.APPS, Categorizer.of("com.x.y", null, null, UadEntry(list = "Google"), false))
        assertEquals(AppGroup.OTHER, Categorizer.of("com.x.y", null, null, null, false))
    }

    @Test
    fun summarize_countsByGroup() {
        val rows = listOf(
            AppRow("a", null, false, true, group = AppGroup.ADS),
            AppRow("b", null, false, true, group = AppGroup.ADS),
            AppRow("c", null, false, true, group = AppGroup.TRACKING),
            AppRow("d", null, false, false, group = AppGroup.OTHER),
        )
        val summary = Categorizer.summarize(rows)
        assertEquals(2, summary[AppGroup.ADS])
        assertEquals(1, summary[AppGroup.TRACKING])
        assertEquals(1, summary[AppGroup.OTHER])
    }
}
