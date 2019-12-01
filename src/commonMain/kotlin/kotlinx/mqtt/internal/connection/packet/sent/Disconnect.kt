package kotlinx.mqtt.internal.connection.packet.sent

import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket

internal class Disconnect : MqttSentPacket() {

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { emptyList<Byte>() }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return false
    }
}