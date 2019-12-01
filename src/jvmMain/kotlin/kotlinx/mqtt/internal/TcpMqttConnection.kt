package kotlinx.mqtt.internal

import kotlinx.io.ByteArrayInputStream
import kotlinx.io.ByteArrayOutputStream
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.mqtt.Logger
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.MqttConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

internal class TcpMqttConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    onConnectionChanged: (Boolean) -> Unit
) : MqttConnection(connectionConfig, logger, onConnectionChanged) {

    private var socket = Socket()

    override var inputStream: InputStream = ByteArrayInputStream(ByteArray(0))

    override var outputStream: OutputStream = ByteArrayOutputStream(0)

    override suspend fun establishConnection(serverUri: String, timeout: Long) {
        clearConnection()
        val uri = URI(serverUri)
        socket = Socket()
        socket.connect(InetSocketAddress(uri.host, uri.port), timeout.toInt())
        inputStream = socket.getInputStream()
        outputStream = socket.getOutputStream()
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