package kotlinx.mqtt.internal.connection.packet.received

internal class PubRec(val packageIdentifier: Short) : MqttReceivedPacket {

    override fun toString(): String {
        return "PubRec(packageIdentifier=$packageIdentifier)"
    }
}