package mbmk.mqtt.internal.connection.packet.sent

import mbmk.mqtt.MqttQos
import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.util.addShort
import mbmk.mqtt.internal.util.addStringWithLength

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