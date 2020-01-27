package mbmk.mqtt.internal.connection.packet

import mbmk.mqtt.MqttMessage
import mbmk.mqtt.MqttQos.AT_LEAST_ONCE
import mbmk.mqtt.MqttQos.EXACTLY_ONCE
import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.received.PubAck
import mbmk.mqtt.internal.connection.packet.received.PubRec
import mbmk.mqtt.internal.connection.packet.sent.MqttSentPacket
import mbmk.mqtt.internal.util.addShort
import mbmk.mqtt.internal.util.addStringWithLength
import mbmk.mqtt.internal.util.shl
import kotlin.experimental.or

internal data class Publish(
    val mqttMessage: MqttMessage,
    override val packetIdentifier: Short
) : MqttSentPacket(), MqttReceivedPacket {

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
        if (mqttMessage.qos == AT_LEAST_ONCE && (receivedPacket as? PubAck)?.packetIdentifier == packetIdentifier) {
            return true
        } else if (mqttMessage.qos == EXACTLY_ONCE && (receivedPacket as? PubRec)?.packetIdentifier == packetIdentifier) {
            return true
        }
        return false
    }
}