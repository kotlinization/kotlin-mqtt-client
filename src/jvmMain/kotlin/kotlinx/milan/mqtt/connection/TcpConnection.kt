package kotlinx.milan.mqtt.connection

import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

internal class TcpConnection : Connection() {

    private var socket: Socket = Socket()

    override val connected: Boolean
        get() = socket.isConnected

    override fun establishConnection(serverUri: String) {
        val uri = URI(serverUri)
        socket.connect(InetSocketAddress(uri.host, uri.port))
    }

    override fun writeMessage(mqttPacket: MqttPacket) {
        socket.getOutputStream().write(mqttPacket.toByteArray())
    }
}