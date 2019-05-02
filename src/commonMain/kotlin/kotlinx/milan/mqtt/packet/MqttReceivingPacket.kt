package kotlinx.milan.mqtt.packet

import kotlinx.io.IOException
import kotlinx.io.InputStream
import readBytes
import toDecodedInt
import kotlin.reflect.KClass

internal abstract class MqttReceivingPacket : MqttPacket()

internal fun InputStream.getPacket(): MqttReceivingPacket {
    val type = read() shr 4
    val size = toDecodedInt()
    val bytes = readBytes(size)
    println("Type: $type")
    val kClass = types[type.toByte()] ?: throw IOException("Unknown type.")
    return bytes.createReceivingPacket(kClass)
}

private fun List<Byte>.createReceivingPacket(kClass: KClass<out MqttPacket>): MqttReceivingPacket {
    return when (kClass) {
        Connack::class -> Connack(this)
        else -> throw IllegalArgumentException("Unknown class: $kClass")
    }
}