package kotlinx.milan.mqtt.packet

import shl
import toEncodedBytes

internal abstract class MqttSendingPacket : MqttPacket {

    protected abstract val variableHeader: List<Byte>

    protected abstract val payload: List<Byte>

    private val remainingLength: List<Byte>
        get() = (variableHeader.size + payload.size).toEncodedBytes()

    private val packetType: Byte
        get() = reverseTypes[this::class] ?: throw IllegalStateException("Class not found.")

    internal fun pack(): List<Byte> {
        val bytes = mutableListOf(packetType shl 4)
        bytes.addAll(remainingLength)
        bytes.addAll(variableHeader)
        bytes.addAll(payload)
        return bytes
    }
}