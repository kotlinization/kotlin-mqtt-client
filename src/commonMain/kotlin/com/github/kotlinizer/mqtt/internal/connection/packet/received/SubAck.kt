package com.github.kotlinizer.mqtt.internal.connection.packet.received

// TODO Decode SubAck response.
internal data class SubAck(
    val responseData: List<Byte>
) : MqttReceivedPacket {

    override val packetIdentifier: Short
        get() = TODO("Not yet implemented")
}