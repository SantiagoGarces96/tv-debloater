package tv.clean.debloater.webos

import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Lee el id del paquete de un .ipk de webOS.
 * Un .ipk es un archivo `ar` con control.tar.gz; dentro, el fichero `control` tiene "Package: <id>".
 */
object IpkReader {

    fun packageId(ipk: File): String? = runCatching {
        ipk.inputStream().buffered().use { input ->
            val controlGz = readArMember(input) { it.startsWith("control.tar") } ?: return null
            val control = readTarMember(GZIPInputStream(controlGz.inputStream())) {
                it.trimStart('.', '/') == "control"
            } ?: return null
            control.toString(Charsets.UTF_8)
                .lineSequence()
                .firstOrNull { it.startsWith("Package:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
        }
    }.getOrNull()

    /** Devuelve el contenido del primer miembro `ar` cuyo nombre cumpla [match]. */
    private fun readArMember(input: InputStream, match: (String) -> Boolean): ByteArray? {
        val magic = ByteArray(8)
        if (input.readNBytesCompat(magic) != 8 || String(magic) != "!<arch>\n") return null
        val header = ByteArray(60)
        while (input.readNBytesCompat(header) == 60) {
            val name = String(header, 0, 16).trim().trimEnd('/')
            val size = String(header, 48, 10).trim().toLongOrNull() ?: return null
            val data = ByteArray(size.toInt())
            if (input.readNBytesCompat(data) != data.size) return null
            if (size % 2 == 1L) input.read() // padding a byte par
            if (match(name)) return data
        }
        return null
    }

    /** Devuelve el contenido del primer fichero del tar cuyo nombre cumpla [match]. */
    private fun readTarMember(input: InputStream, match: (String) -> Boolean): ByteArray? {
        val block = ByteArray(512)
        while (input.readNBytesCompat(block) == 512) {
            if (block.all { it.toInt() == 0 }) break
            val name = String(block, 0, 100).trim { it <= ' ' || it == '\u0000' }
            val sizeOctal = String(block, 124, 12).trim { it <= ' ' || it == '\u0000' }
            val size = sizeOctal.toLongOrNull(8) ?: 0L
            val padded = ((size + 511) / 512 * 512).toInt()
            val data = ByteArray(padded)
            if (input.readNBytesCompat(data) != padded) return null
            if (name.isNotEmpty() && match(name)) return data.copyOf(size.toInt())
        }
        return null
    }

    private fun InputStream.readNBytesCompat(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val r = read(buffer, total, buffer.size - total)
            if (r < 0) break
            total += r
        }
        return total
    }
}
