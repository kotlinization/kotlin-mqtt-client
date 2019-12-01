package kotlinx.mqtt.internal.connection

import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.sent.MqttSentPacket

internal data class PacketResponse(
    val sentPacket: MqttSentPacket,
    val onResponse: suspend (MqttReceivedPacket) -> Unit
)