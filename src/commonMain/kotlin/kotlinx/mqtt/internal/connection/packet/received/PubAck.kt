package kotlinx.mqtt.internal.connection.packet.received

class PubAck(val packageIdentifier: Short) : MqttReceivedPacket {

    override fun toString(): String {
        return "PubAck(packageIdentifier=$packageIdentifier)"
    }
}