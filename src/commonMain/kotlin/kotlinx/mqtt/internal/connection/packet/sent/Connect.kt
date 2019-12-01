package kotlinx.mqtt.internal.connection.packet.sent

import addByteList
import addShort
import addStringWithLength
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.packet.received.ConnAck
import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import shl
import kotlin.experimental.or

internal class Connect(private val connectionConfig: MqttConnectionConfig) : MqttSentPacket() {

    override val variableHeader: List<Byte> by lazy {
        var flags: Byte = 0
        if (connectionConfig.cleanSession) {
            flags = flags or 0b0000_0010
        }
        if (connectionConfig.password != null) {
            flags = flags or 0b0100_0000
        }
        if (connectionConfig.username != null) {
            flags = flags or 0b1000_0000.toByte()
        }
        if (connectionConfig.willMessage != null) {
            flags = flags or 0b0000_0100
            flags = flags or if (connectionConfig.willMessage.retain) 0b0100_0000 else 0
            flags = flags or (connectionConfig.willMessage.qos.ordinal.toByte() shl 3)
        }
        mutableListOf<Byte>().apply {
            addStringWithLength("MQTT")
            add(4)
            add(flags)
            addShort(connectionConfig.keepAlive)
        }
    }

    override val payload: List<Byte> by lazy {
        mutableListOf<Byte>().apply {
            addStringWithLength(connectionConfig.clientId)
            connectionConfig.willMessage?.let {
                addStringWithLength(it.topic)
                addByteList(it.message)
            }
            connectionConfig.username?.let { addStringWithLength(it) }
            connectionConfig.password?.let { addStringWithLength(it) }

        }
    }

    override fun isResponse(receivedPacket: MqttReceivedPacket): Boolean {
        return receivedPacket is ConnAck
    }

    override fun toString(): String {
        return "Connect(connectionConfig=$connectionConfig)"
    }
}
