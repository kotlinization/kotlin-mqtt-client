import kotlinx.io.ByteBuffer
import kotlinx.serialization.toUtf8Bytes
import kotlin.experimental.or


internal fun MutableList<Byte>.addShort(short: Short) {
    addAll(short.toByteArray().toTypedArray())
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


internal fun Int.toNonNormativeBytes(): List<Byte> {
    val bytes = mutableListOf<Byte>()
    var x = this
    do {
        val encodedByte = (x % 128).toByte()
        x /= 128
        bytes.add(if (x > 0) encodedByte or 128.toByte() else encodedByte)
    } while (x > 0)
    return bytes
}