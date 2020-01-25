package mbmk.mqtt.internal.connection.packet

import mbmk.mqtt.internal.connection.packet.received.*
import mbmk.mqtt.internal.connection.packet.sent.Connect
import mbmk.mqtt.internal.connection.packet.sent.Disconnect
import mbmk.mqtt.internal.connection.packet.sent.PingReq
import mbmk.mqtt.internal.connection.packet.sent.PubRel


internal interface MqttPacket

internal val types = mapOf(
    1 to Connect::class,
    2 to ConnAck::class,
    3 to Publish::class,
    4 to PubAck::class,
    5 to PubRec::class,
    6 to PubRel::class,
    7 to PubComp::class,
    12 to PingReq::class,
    13 to PingResp::class,
    14 to Disconnect::class
)

internal val reverseTypes by lazy {
    types.map { it.value to it.key.toByte() }.toMap()
}
