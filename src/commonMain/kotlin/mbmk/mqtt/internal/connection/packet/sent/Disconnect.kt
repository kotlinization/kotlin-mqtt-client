package mbmk.mqtt.internal.connection.packet.sent

import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket


internal class Disconnect : MqttSentPacket() {

    override val fixedHeader: Byte = 0

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { emptyList<Byte>() }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return false
    }

    override fun toString(): String {
        return "Disconnect()"
    }
}