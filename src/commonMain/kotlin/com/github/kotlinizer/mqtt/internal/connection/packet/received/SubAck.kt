package com.github.kotlinizer.mqtt.internal.connection.packet.received

internal data class SubAck(
    val responseData: List<Byte>
) : MqttReceivedPacket {
    //    val returnCodes: List<MqttQos?>
    override val packetIdentifier: Short
        get() = TODO("Not yet implemented")
}