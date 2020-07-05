package com.github.kotlinizer.mqtt.internal.connection.packet.received

internal class PingResp : MqttReceivedPacket {

    override val packetIdentifier: Short = 0

    override fun toString(): String {
        return "PingResp()"
    }
}