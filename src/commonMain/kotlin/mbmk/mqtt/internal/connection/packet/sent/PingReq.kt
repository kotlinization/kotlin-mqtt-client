package mbmk.mqtt.internal.connection.packet.sent

import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.received.PingResp

internal class PingReq : MqttSentPacket() {

    override val fixedHeader: Byte = 0

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { emptyList<Byte>() }

    override val packetIdentifier: Short = 0

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return receivedPacket is PingResp
    }

    override fun toString(): String {
        return "PingReq()"
    }
}