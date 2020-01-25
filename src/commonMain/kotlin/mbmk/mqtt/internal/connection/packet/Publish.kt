package mbmk.mqtt.internal.connection.packet

import addShort
import addStringWithLength
import mbmk.mqtt.MqttMessage
import mbmk.mqtt.MqttQos.AT_LEAST_ONCE
import mbmk.mqtt.MqttQos.EXACTLY_ONCE
import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.received.PubAck
import mbmk.mqtt.internal.connection.packet.received.PubRec
import mbmk.mqtt.internal.connection.packet.sent.MqttSentPacket
import shl
import kotlin.experimental.or

internal class Publish(
    private val mqttMessage: MqttMessage,
    val packetIdentifier: Short? = null
) : MqttSentPacket(), MqttReceivedPacket {

    override val fixedHeader: Byte by lazy {
        mqttMessage.constrainedQos.shl(1).or(if (mqttMessage.retain) 0b0000_0001 else 0)
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
        if (mqttMessage.constrainedQos == AT_LEAST_ONCE && (receivedPacket as? PubAck)?.packageIdentifier == packetIdentifier) {
            return true
        } else if (mqttMessage.constrainedQos == EXACTLY_ONCE && (receivedPacket as? PubRec)?.packageIdentifier == packetIdentifier) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "Publish(packetIdentifier=$packetIdentifier, qos=${mqttMessage.constrainedQos}, fixedHeader=$fixedHeader)"
    }
}