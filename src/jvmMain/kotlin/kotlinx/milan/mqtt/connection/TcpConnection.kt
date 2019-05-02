package kotlinx.milan.mqtt.connection

import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

internal class TcpConnection : Connection() {

    override val connected: Boolean
        get() = socket.isConnected

    private var socket: Socket = Socket()

    override val inputStream: InputStream
        get() = socket.getInputStream()

    override val outputStream: OutputStream
        get() = socket.getOutputStream()

    override fun establishConnection(serverUri: String) {
        val uri = URI(serverUri)
        socket.connect(InetSocketAddress(uri.host, uri.port))
    }

}