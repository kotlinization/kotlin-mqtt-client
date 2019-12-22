package kotlinx.mqtt.internal.connection.packet

import addShort
import addStringWithLength
import kotlinx.mqtt.MqttMessage
import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.sent.MqttSentPacket
import shl
import kotlin.experimental.or

internal class Publish(mqttMessage: MqttMessage, private val packetIdentifier: Short? = null) : MqttSentPacket(), MqttReceivedPacket {

    override val fixedHeader: Byte by lazy {
        mqttMessage.constrainedQos.shl(1) or if (mqttMessage.retain) 0b0000_0001 else 0b00
    }

    override val variableHeader: List<Byte> by lazy {
        mutableListOf<Byte>().apply {
            addStringWithLength(mqttMessage.topic)
            if (packetIdentifier != null) {
                addShort(packetIdentifier)
            }
        }
    }

    override val payload: List<Byte> = mqttMessage.message

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return false
    }

    override fun toString(): String {
        return "Publish(packetIdentifier=$packetIdentifier)"
    }
}