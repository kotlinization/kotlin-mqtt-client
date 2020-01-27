package mbmk.mqtt.internal.connection.packet.received

import kotlinx.io.IOException

internal class ConnAck(bytes: List<Byte>) : MqttReceivedPacket {

    private val returnedCode: Int by lazy {
        bytes.getOrNull(1)?.toInt() ?: throw IllegalArgumentException("Bytes must have at least 2 bytes.")
    }

    val error: IOException? by lazy {
        when (returnedCode) {
            0 -> null
            1 -> IOException("The Server does not support the level of the MQTT protocol requested by the Client.")
            2 -> IOException("The Client identifier is correct UTF-8 but not allowed by the Server.")
            3 -> IOException("The Network Connection has been made but the MQTT service is unavailable.")
            4 -> IOException("The data in the user name or password is malformed.")
            5 -> IOException("The Client is not authorized to connect.")
            else -> IOException("Unknown error.")
        }
    }

    override val packetIdentifier: Short = 0

    override fun toString(): String {
        return "ConnAck(returnedCode=$returnedCode)"
    }
}