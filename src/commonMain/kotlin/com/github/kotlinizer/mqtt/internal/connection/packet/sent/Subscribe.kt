package com.github.kotlinizer.mqtt.internal.connection.packet.sent

import com.github.kotlinizer.mqtt.MqttQos
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.util.addShort
import com.github.kotlinizer.mqtt.internal.util.addStringWithLength

internal data class Subscribe(
    override val packetIdentifier: Short,
    private val topicAndQos: Map<String, MqttQos>
) : MqttSentPacket() {

    override val fixedHeader: Byte = 2

    override val variableHeader: List<Byte> by lazy {
        mutableListOf<Byte>().apply {
            addShort(packetIdentifier)
        }
    }

    override val payload: List<Byte> by lazy {
        topicAndQos.flatMap { (topic, qos) ->
            mutableListOf<Byte>().apply {
                addStringWithLength(topic)
                add(qos.ordinal.toByte())
            }
        }
    }

    override fun isResponse(receivedPacket: MqttReceivedPacket) = false
}