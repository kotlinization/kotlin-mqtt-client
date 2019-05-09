package kotlinx.mqtt.internal.connection

import kotlinx.mqtt.internal.connection.packet.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.MqttSentPacket

internal data class PacketResponse(
    val sentPacket: MqttSentPacket,
    val onResponse: suspend (MqttReceivedPacket) -> Unit
)