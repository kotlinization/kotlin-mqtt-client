package kotlinx.milan.mqtt.connection

import kotlinx.milan.mqtt.MqttConnectionConfig
import java.net.InetSocketAddress
import java.net.URI
import java.nio.channels.SocketChannel

internal class TcpConnection(private val connectionConfig: MqttConnectionConfig) : Connection() {

    private val socketChannel = SocketChannel.open().apply {
        configureBlocking(true)
    }

    override val connected: Boolean
        get() = socketChannel.isConnected

    override fun establishConnection() : Boolean{
        val uri = URI(connectionConfig.serverUri)
        val socketChanel = SocketChannel.open()
        return socketChanel.connect(InetSocketAddress(uri.host, uri.port))
    }
}