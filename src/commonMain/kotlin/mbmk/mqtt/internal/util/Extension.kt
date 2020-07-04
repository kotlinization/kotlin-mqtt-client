package mbmk.mqtt.internal.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import mbmk.mqtt.MQTTException
import mbmk.mqtt.StopFlowCollection
import kotlin.experimental.and
import kotlin.experimental.or

internal fun MutableList<Byte>.addShort(short: Short) {
    addAll(short.toByteList())
}

internal fun MutableList<Byte>.addByteList(list: List<Byte>) {
    addShort(list.size.toShort())
    addAll(list)
}

@OptIn(ExperimentalStdlibApi::class)
internal fun MutableList<Byte>.addStringWithLength(string: String) {
    val bytes = string.encodeToByteArray().toTypedArray()
    addAll(bytes.size.toShort().toByteList())
    addAll(bytes)
}


internal fun Short.toByteList(): List<Byte> {
    return listOf(
        toInt().shr(8).toByte(),
        toByte()
    )
}

internal fun List<Byte>.toShort(): Short {
    if (size < 2) throw IllegalArgumentException("List must have at least 2 elements.")
    return (get(0).toShort() + get(1).shl(8).toShort()).toShort()
}

internal infix fun Byte.shl(count: Int): Byte {
    return toInt().shl(count).toByte()
}

internal infix fun Byte.shr(count: Int): Byte {
    return toInt().shr(count).toByte()
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

internal suspend fun Flow<Byte>.toDecodedInt(): Int {
    var multiplier = 1
    var value = 0
    try {
        collect { encodedByte ->
            value += (encodedByte and 127) * multiplier
            multiplier *= 128
            if (multiplier > 128 * 128 * 128) {
                throw MQTTException("Malformed remaining length.")
            }
            if ((encodedByte and MASK) == STOP)
                throw StopFlowCollection
        }
    } catch (sfc: StopFlowCollection) {
        //This is expected behavior
    }
    return value
}

internal fun Throwable.throwIfCancel() {
    if (this is CancellationException) {
        throw this
    }
}