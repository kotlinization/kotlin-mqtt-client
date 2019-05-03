package kotlinx.mqtt.packet

internal class Disconnect : MqttSendingPacket() {

    override val variableHeader by lazy { emptyList<Byte>() }

    override val payload by lazy { emptyList<Byte>() }
}