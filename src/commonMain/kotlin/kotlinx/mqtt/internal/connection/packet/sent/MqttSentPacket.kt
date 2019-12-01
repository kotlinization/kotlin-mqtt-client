package kotlinx.mqtt.internal.connection.packet.sent

import kotlinx.mqtt.internal.connection.packet.MqttPacket
import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.reverseTypes
import shl
import toEncodedBytes

internal abstract class MqttSentPacket : MqttPacket {

    protected abstract val variableHeader: List<Byte>

    protected abstract val payload: List<Byte>

    private val remainingLength: List<Byte>
        get() = (variableHeader.size + payload.size).toEncodedBytes()

    private val packetType: Byte
        get() = reverseTypes[this::class] ?: throw IllegalStateException("Class not found.")

    fun pack(): List<Byte> {
        val bytes = mutableListOf(packetType shl 4)
        bytes.addAll(remainingLength)
        bytes.addAll(variableHeader)
        bytes.addAll(payload)
        return bytes
    }

    abstract fun isResponse(receivedPacket: MqttReceivedPacket): Boolean
}