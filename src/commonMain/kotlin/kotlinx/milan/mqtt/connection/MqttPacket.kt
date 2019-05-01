package kotlinx.milan.mqtt.connection

import addShort
import addStringWithLength
import kotlinx.milan.mqtt.MqttConnectionConfig
import shl
import toNonNormativeBytes
import kotlin.experimental.or


sealed class MqttPacket {

    abstract val packetType: Byte

    abstract val variableHeader: List<Byte>

    abstract val payload: List<Byte>

    private val remainingLength: List<Byte>
        get() = (variableHeader.size + payload.size).toNonNormativeBytes()

    fun toByteArray(): ByteArray {
        val bytes = mutableListOf(packetType shl 4)
        bytes.addAll(remainingLength)
        bytes.addAll(variableHeader)
        bytes.addAll(payload)
        return bytes.toByteArray()
    }
}

class Connect(connectionConfig: MqttConnectionConfig) : MqttPacket() {

    override val packetType: Byte = 1

    override val variableHeader: List<Byte> by lazy {
        var flags: Byte = 0
        if (connectionConfig.cleanSession) {
            flags = flags or 2
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
        }
    }

//    private val willTopic: String? = null
//    private val willMessage = ""
//    private val willRetain: Boolean = false
//    private val willQos: Byte = 0
//    private val userName: String? = null
//    private val password: String? = null

//    fun toByteArrays(): ByteArray {
//        if (userName != null) {
//            flags = flags or 80
//        }
//        if (password != null) {
//            flags = flags or 0x40
//        }
//        if (willTopic != null && willMessage != null) {
//            flags = flags or 0x04
//            if (willRetain) {
//                flags = flags or 0x20
//            }
//            flags = flags or (willQos shl 3 and 0x18)
//        }\
//        if (clientId.isNotEmpty()) {
//            bytes.addStringWithLength(clientId)
//        }
//        if (willTopic != null && willMessage != null) {
//            MessageSupport.writeUTF(os, willTopic)
//            MessageSupport.writeUTF(os, willMessage)
//        }
//        if (!userName.isNullOrBlank()) {
//            bytes.addStringWithLength(userName)
//        }
//        if (!password.isNullOrBlank()) {
//            bytes.addStringWithLength(password)
//        }
//    }
}
