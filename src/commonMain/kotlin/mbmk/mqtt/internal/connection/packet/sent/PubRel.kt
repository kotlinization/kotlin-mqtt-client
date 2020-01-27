package mbmk.mqtt.internal.connection.packet.sent

import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.received.PubComp
import mbmk.mqtt.internal.util.toByteArray

internal data class PubRel(override val packetIdentifier: Short) : MqttSentPacket() {

    override val fixedHeader: Byte = 2

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { packetIdentifier.toByteArray().toList() }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return receivedPacket is PubComp
    }
}