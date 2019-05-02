import kotlinx.io.ByteBuffer
import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlinx.serialization.toUtf8Bytes
import kotlin.experimental.and
import kotlin.experimental.or

internal fun MutableList<Byte>.addShort(short: Short) {
    addAll(short.toByteArray().toTypedArray())
}

internal fun MutableList<Byte>.addByteList(list: List<Byte>) {
    addShort(list.size.toShort())
    addAll(list)
}

internal fun MutableList<Byte>.addStringWithLength(string: String) {
    val bytes = string.toUtf8Bytes().toTypedArray()
    addAll(bytes.size.toShort().toByteArray().toTypedArray())
    addAll(bytes)
}

internal fun Short.toByteArray(): ByteArray {
    return ByteBuffer.allocate(2).putShort(this).array()
}

internal fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(4).putInt(this).array()
}

internal infix fun Byte.shl(count: Int): Byte {
    return toInt().shl(count).toByte()
}

internal fun Int.toEncodedBytes(): List<Byte> {
    val bytes = mutableListOf<Byte>()
    var x = this
    do {
        val encodedByte = (x % 128).toByte()
        x /= 128
        bytes.add(if (x > 0) encodedByte or 128.toByte() else encodedByte)
    } while (x > 0)
    return bytes
}

private const val MASK = 128.toByte()

private const val STOP = 0.toByte()

/**
 * @throws Throwable
 */
internal fun InputStream.toDecodedInt(): Int {
    var multiplier = 1
    var value = 0
    do {
        val encodedByte = read().toByte()
        value += (encodedByte and 127) * multiplier
        multiplier *= 128
        if (multiplier > 128 * 128 * 128) {
            throw IOException("Malformed remaining length.")
        }
    } while ((encodedByte and MASK) != STOP)
    return value
}

/**
 * @throws Throwable
 */
internal fun InputStream.readBytes(size: Int): List<Byte> {
    return mutableListOf<Byte>().apply {
        repeat(size) {
            val byte = read().takeIf { it != -1 }?.toByte() ?: throw IOException("End of stream reached.")
            add(byte)
        }
    }
}