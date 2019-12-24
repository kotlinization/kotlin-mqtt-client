package kotlinx.mqtt.internal.connection.packet.sent

import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.received.PubComp
import toByteArray

internal class PubRel(val packageIdentifier: Short) : MqttSentPacket() {

    override val fixedHeader: Byte = 2

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { packageIdentifier.toByteArray().toList() }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return receivedPacket is PubComp
    }

    override fun toString(): String {
        return "PubRel(packageIdentifier=$packageIdentifier)"
    }
}