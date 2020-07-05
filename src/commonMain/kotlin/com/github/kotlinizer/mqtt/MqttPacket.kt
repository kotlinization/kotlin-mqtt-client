package com.github.kotlinizer.mqtt

import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import com.github.kotlinizer.mqtt.internal.connection.packet.received.*
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.*


interface MqttPacket {

    /**
     * Packets that don' use identifier has value [0].
     */
    val packetIdentifier: Short
}

internal val types = mapOf(
    1 to Connect::class,
    2 to ConnAck::class,
    3 to Publish::class,
    4 to PubAck::class,
    5 to PubRec::class,
    6 to PubRel::class,
    7 to PubComp::class,
    8 to Subscribe::class,
    12 to PingReq::class,
    13 to PingResp::class,
    14 to Disconnect::class
)

internal val reverseTypes by lazy {
    types.map { it.value to it.key.toByte() }.toMap()
}
