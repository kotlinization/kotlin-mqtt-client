package kotlinx.mqtt.internal.connection.packet.received

import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlinx.mqtt.internal.connection.packet.MqttPacket
import kotlinx.mqtt.internal.connection.packet.types
import readBytes
import toDecodedInt
import toShort

internal interface MqttReceivedPacket : MqttPacket

internal suspend fun InputStream.getPacket(): MqttReceivedPacket {
    val type = read() shr 4
    val size = toDecodedInt()
    val bytes = readBytes(size)
    val kClass = types[type] ?: throw IOException("Unknown type.")
    return when (kClass) {
        ConnAck::class -> ConnAck(bytes)
        PingResp::class -> PingResp()
        PubAck::class -> PubAck(bytes.toShort())
        else -> throw IllegalArgumentException("Unknown class: $kClass")
    }
}