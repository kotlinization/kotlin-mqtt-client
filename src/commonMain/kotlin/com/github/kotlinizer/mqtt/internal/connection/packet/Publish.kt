package com.github.kotlinizer.mqtt.internal.connection.packet

import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.MqttQos
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.received.PubAck
import com.github.kotlinizer.mqtt.internal.connection.packet.received.PubRec
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.MqttSentPacket
import com.github.kotlinizer.mqtt.internal.util.addShort
import com.github.kotlinizer.mqtt.internal.util.addStringWithLength
import com.github.kotlinizer.mqtt.internal.util.shl
import kotlin.experimental.or

internal data class Publish(
    val mqttMessage: MqttMessage,
    override val packetIdentifier: Short
) : MqttSentPacket(), MqttReceivedPacket {

    constructor(bytes: List<Byte>) : this(
        MqttMessage("", bytes),
        0.toShort()
    )

    override val fixedHeader: Byte by lazy {
        mqttMessage.qos.ordinal.toByte().shl(1).or(if (mqttMessage.retain) 0b0000_0001 else 0)
    }

    override val variableHeader: List<Byte> by lazy {
        mutableListOf<Byte>().apply {
            addStringWithLength(mqttMessage.topic)
            addShort(packetIdentifier)
        }
    }

    override val payload: List<Byte> = mqttMessage.message

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        if (mqttMessage.qos == MqttQos.AT_LEAST_ONCE && (receivedPacket as? PubAck)?.packetIdentifier == packetIdentifier) {
            return true
        } else if (mqttMessage.qos == MqttQos.EXACTLY_ONCE && (receivedPacket as? PubRec)?.packetIdentifier == packetIdentifier) {
            return true
        }
        return false
    }
}