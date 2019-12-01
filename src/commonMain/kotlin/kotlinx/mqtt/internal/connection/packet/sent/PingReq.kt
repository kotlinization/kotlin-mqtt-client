package kotlinx.mqtt.internal.connection.packet.sent

import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.received.PingResp

internal class PingReq : MqttSentPacket() {

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { emptyList<Byte>() }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return receivedPacket is PingResp
    }

    override fun toString(): String {
        return "PingReq()"
    }
}