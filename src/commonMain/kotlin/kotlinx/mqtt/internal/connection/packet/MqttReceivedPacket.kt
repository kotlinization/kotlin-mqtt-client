package kotlinx.mqtt.internal.connection.packet

import kotlinx.io.IOException
import kotlinx.io.InputStream
import readBytes
import toDecodedInt
import kotlin.reflect.KClass

internal interface MqttReceivedPacket : MqttPacket

internal suspend fun InputStream.getPacket(): MqttReceivedPacket {
    val type = read() shr 4
    val size = toDecodedInt()
    val bytes = readBytes(size)
    val kClass = types[type.toByte()] ?: throw IOException("Unknown type.")
    return bytes.createReceivingPacket(kClass)
}

private fun List<Byte>.createReceivingPacket(kClass: KClass<out MqttPacket>): MqttReceivedPacket {
    return when (kClass) {
        ConnAck::class -> ConnAck(this)
        else -> throw IllegalArgumentException("Unknown class: $kClass")
    }
}