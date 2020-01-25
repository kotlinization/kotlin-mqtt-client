package mbmk.mqtt.internal.connection.packet.received

internal class PubComp(val packageIdentifier: Short) : MqttReceivedPacket {

    override fun toString(): String {
        return "PubComp(packageIdentifier=$packageIdentifier)"
    }
}