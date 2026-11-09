package tv.clean.debloater

import org.junit.Assert.assertEquals
import org.junit.Test
import tv.clean.debloater.webos.IpkReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

class IpkReaderTest {

    @Test
    fun packageId_readsControlPackageField() {
        val control = "Package: com.test.app\nVersion: 1.0.0\n".toByteArray()
        val controlTar = tar("control", control)
        val controlTarGz = gzip(controlTar)
        val ipkBytes = ar("control.tar.gz", controlTarGz)

        val file = File.createTempFile("sample", ".ipk")
        file.writeBytes(ipkBytes)
        file.deleteOnExit()

        assertEquals("com.test.app", IpkReader.packageId(file))
    }

    private fun tar(name: String, data: ByteArray): ByteArray {
        val header = ByteArray(512)
        name.toByteArray().copyInto(header, 0)
        String.format("%011o", data.size).toByteArray().copyInto(header, 124)
        header[156] = '0'.code.toByte()
        val out = ByteArrayOutputStream()
        out.write(header)
        out.write(data)
        val pad = (data.size + 511) / 512 * 512 - data.size
        repeat(pad) { out.write(0) }
        out.write(ByteArray(512)) // bloque de fin
        return out.toByteArray()
    }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun ar(name: String, data: ByteArray): ByteArray {
        val header = ByteArray(60) { ' '.code.toByte() }
        name.toByteArray().copyInto(header, 0)
        data.size.toString().toByteArray().copyInto(header, 48)
        header[58] = 0x60
        header[59] = 0x0A
        val out = ByteArrayOutputStream()
        out.write("!<arch>\n".toByteArray())
        out.write(header)
        out.write(data)
        if (data.size % 2 == 1) out.write(0x0A)
        return out.toByteArray()
    }
}
