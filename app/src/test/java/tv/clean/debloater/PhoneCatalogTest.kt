package tv.clean.debloater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.clean.debloater.phone.PhoneCatalog

class PhoneCatalogTest {

    @Test
    fun noRepeatedPackages() {
        assertEquals(PhoneCatalog.apps.size, PhoneCatalog.apps.map { it.pkg }.toSet().size)
    }

    @Test
    fun neverTouchesEssentials() {
        listOf(
            "com.android.systemui", "com.android.settings", "com.android.phone", "com.miui.home",
            "com.mi.android.globallauncher", "com.miui.securitycenter", "com.google.android.gms",
            "com.android.vending", "com.sec.android.app.launcher", "com.android.contacts", "com.android.mms",
        ).forEach { assertFalse(it, PhoneCatalog.isAllowed(it)) }
    }

    @Test
    fun xiaomiHomeExtrasAreNotRecommended() {
        val extras = PhoneCatalog.apps.filter { it.pkg in setOf("com.mi.globalminusscreen", "com.mi.appfinder") }
        assertEquals(2, extras.size)
        assertTrue(extras.none { it.recommended })
    }

    @Test
    fun shellOnlyAcceptsListedCommands() {
        val pkg = PhoneCatalog.apps.first().pkg
        assertTrue(PhoneCatalog.isAllowedCommand(PhoneCatalog.removeCommand(pkg)))
        assertTrue(PhoneCatalog.isAllowedCommand(PhoneCatalog.restoreCommand(pkg)))
        assertFalse(PhoneCatalog.isAllowedCommand(PhoneCatalog.removeCommand("com.android.systemui")))
        assertFalse(PhoneCatalog.isAllowedCommand(PhoneCatalog.removeCommand(pkg) + "; reboot"))
        assertFalse(PhoneCatalog.isAllowedCommand("rm -rf /"))
    }
}
