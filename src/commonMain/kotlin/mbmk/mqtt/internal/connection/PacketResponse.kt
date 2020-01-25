package mbmk.mqtt.internal.connection

import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.sent.MqttSentPacket

internal data class PacketResponse(
    val sentPacket: MqttSentPacket,
    val onResponse: suspend (MqttReceivedPacket) -> Unit
)