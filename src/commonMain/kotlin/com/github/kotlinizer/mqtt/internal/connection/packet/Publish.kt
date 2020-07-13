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
import com.github.kotlinizer.mqtt.internal.util.toShort
import kotlin.experimental.and
import kotlin.experimental.or

internal data class Publish(
    override val packetIdentifier: Short,
    val mqttMessage: MqttMessage
) : MqttSentPacket(), MqttReceivedPacket {

    companion object {

        @OptIn(ExperimentalStdlibApi::class)
        fun receivePacket(header: Byte, bytes: List<Byte>): Publish {
            val topicLength = bytes.subList(0, 2).toShort()
            val topic = bytes.subList(2, 2 + topicLength).toByteArray().decodeToString()
            val identifier = bytes.subList(2 + topicLength, 4 + topicLength).toShort()
            val message = bytes.subList(4 + topicLength, bytes.size).toByteArray().decodeToString()
            val retain = header.and(0b0000_0001) == 1.toByte()
            return Publish(identifier, MqttMessage(topic, message, retain = retain))
        }
    }

    override val fixedHeader: Byte by lazy {
        mqttMessage.qos.ordinal.toByte().shl(1).or(if (mqttMessage.retain) 0b0000_0001 else 0)
    }

    override val variableHeader: List<Byte> by lazy {
        mutableListOf<Byte>().apply {
            addStringWithLength(mqttMessage.topic)
            addShort(packetIdentifier)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override val payload: List<Byte> by lazy {
        mqttMessage.messageBytes
    }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        if (mqttMessage.qos == MqttQos.AT_LEAST_ONCE && (receivedPacket as? PubAck)?.packetIdentifier == packetIdentifier) {
            return true
        } else if (mqttMessage.qos == MqttQos.EXACTLY_ONCE && (receivedPacket as? PubRec)?.packetIdentifier == packetIdentifier) {
            return true
        }
        return false
    }
}