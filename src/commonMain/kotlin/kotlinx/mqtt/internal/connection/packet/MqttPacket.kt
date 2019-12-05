package kotlinx.mqtt.internal.connection.packet

import kotlinx.mqtt.internal.connection.packet.received.ConnAck
import kotlinx.mqtt.internal.connection.packet.received.PingResp
import kotlinx.mqtt.internal.connection.packet.sent.Connect
import kotlinx.mqtt.internal.connection.packet.sent.Disconnect
import kotlinx.mqtt.internal.connection.packet.sent.PingReq


internal interface MqttPacket

internal val types = mapOf(
    1 to Connect::class,
    2 to ConnAck::class,
    3 to Publish::class,
    12 to PingReq::class,
    13 to PingResp::class,
    14 to Disconnect::class
)

internal val reverseTypes by lazy {
    types.map { it.value to it.key.toByte() }.toMap()
}
