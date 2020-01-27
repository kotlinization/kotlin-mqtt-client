package mbmk.mqtt.internal.connection.packet.received

internal data class PubAck(override val packetIdentifier: Short) : MqttReceivedPacket