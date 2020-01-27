package mbmk.mqtt.internal.connection.packet.received

import kotlinx.io.IOException
import kotlinx.io.InputStream
import mbmk.mqtt.MqttPacket
import mbmk.mqtt.types
import mbmk.mqtt.internal.util.readBytes
import mbmk.mqtt.internal.util.toDecodedInt
import mbmk.mqtt.internal.util.toShort

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
        PubRec::class -> PubRec(bytes.toShort())
        PubComp::class -> PubComp(bytes.toShort())
        else -> throw IllegalArgumentException("Unknown class: $kClass")
    }
}
