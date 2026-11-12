package tv.clean.debloater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.clean.debloater.net.ApkDownloader
import java.io.File

class DownloaderTest {

    @Test
    fun toUrl_codesUrlsAndEmpty() {
        assertEquals("https://aftv.news/12345", ApkDownloader.toUrl("12345"))
        assertEquals("https://aftv.news/12345", ApkDownloader.toUrl("  12345 "))
        assertEquals("https://example.com/app.apk", ApkDownloader.toUrl("example.com/app.apk"))
        assertEquals("https://x.y/a.apk", ApkDownloader.toUrl("https://x.y/a.apk"))
        assertNull(ApkDownloader.toUrl("   "))
    }

    @Test
    fun isCode_onlyDigits() {
        assertTrue(ApkDownloader.isCode("6330120"))
        assertFalse(ApkDownloader.isCode("abc123"))
        assertFalse(ApkDownloader.isCode(""))
    }

    @Test
    fun looksLikeApk_detectsZipSignature() {
        val apk = File.createTempFile("realapk", ".apk").apply { writeBytes(byteArrayOf(0x50, 0x4B, 3, 4, 9, 9)); deleteOnExit() }
        val html = File.createTempFile("page", ".apk").apply { writeText("<html>no</html>"); deleteOnExit() }
        assertTrue(ApkDownloader.looksLikeApk(apk))
        assertFalse(ApkDownloader.looksLikeApk(html))
    }
}
