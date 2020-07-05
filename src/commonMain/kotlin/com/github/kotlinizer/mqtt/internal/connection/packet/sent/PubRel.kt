package com.github.kotlinizer.mqtt.internal.connection.packet.sent

import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.received.PubComp
import com.github.kotlinizer.mqtt.internal.util.toByteList

internal data class PubRel(override val packetIdentifier: Short) : MqttSentPacket() {

    override val fixedHeader: Byte = 2

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { packetIdentifier.toByteList() }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return receivedPacket is PubComp
    }
}