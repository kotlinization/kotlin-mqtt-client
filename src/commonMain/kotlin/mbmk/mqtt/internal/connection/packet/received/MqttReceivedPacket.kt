package mbmk.mqtt.internal.connection.packet.received

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import mbmk.mqtt.MQTTException
import mbmk.mqtt.MqttPacket
import mbmk.mqtt.internal.util.shr
import mbmk.mqtt.internal.util.toDecodedInt
import mbmk.mqtt.internal.util.toShort
import mbmk.mqtt.types

internal interface MqttReceivedPacket : MqttPacket

internal suspend fun Flow<Byte>.getPacket(): MqttReceivedPacket {
    val type = take(1).first() shr 4
    val size = toDecodedInt()
    val bytes = take(size).toList()
    val kClass = types[type.toInt()] ?: throw MQTTException("Unknown type.")
    return when (kClass) {
        ConnAck::class -> ConnAck(bytes)
        PingResp::class -> PingResp()
        PubAck::class -> PubAck(bytes.toShort())
        PubRec::class -> PubRec(bytes.toShort())
        PubComp::class -> PubComp(bytes.toShort())
        else -> throw IllegalArgumentException("Unknown class: $kClass")
    }
}
