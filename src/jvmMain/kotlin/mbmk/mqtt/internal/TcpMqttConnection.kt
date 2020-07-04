package mbmk.mqtt.internal

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import mbmk.mqtt.Logger
import mbmk.mqtt.MqttConnectionConfig
import mbmk.mqtt.internal.connection.MqttConnection
import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.received.getPacket
import mbmk.mqtt.internal.connection.packet.sent.MqttSentPacket
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

internal class TcpMqttConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    onConnectionChanged: (Boolean) -> Unit
) : MqttConnection(connectionConfig, logger, onConnectionChanged) {

    private var socket = Socket()

    private var inputStream: InputStream = ByteArrayInputStream(ByteArray(0))

    private var outputStream: OutputStream = ByteArrayOutputStream(0)

    override suspend fun establishConnection(serverUri: String, timeout: Long) {
        clearConnection()
        val uri = URI(serverUri)
        socket = Socket()
        withContext(IO) {
            socket.connect(InetSocketAddress(uri.host, uri.port), timeout.toInt())
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        }
    }

    override suspend fun writePacket(packet: MqttSentPacket) {
        withContext(IO) {
            outputStream.write(packet.pack().toByteArray())
        }
    }

    override suspend fun readPacket(): MqttReceivedPacket {
        return flow {
            emit(inputStream.read().toByte())
        }.getPacket()
    }

    override fun clearConnection() {
        runCatching {
            socket.use {
                outputStream.flush()
                socket.shutdownInput()
                socket.shutdownOutput()
            }
        }
    }
}