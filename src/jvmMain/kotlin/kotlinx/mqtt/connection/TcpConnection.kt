package kotlinx.mqtt.connection

import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

internal class TcpConnection(onConnectionChanged: (Boolean) -> Unit) : Connection(onConnectionChanged) {

    private var socket = Socket()

    override val inputStream: InputStream
        get() = socket.getInputStream()

    override val outputStream: OutputStream
        get() = socket.getOutputStream()

    override fun establishConnection(serverUri: String, timeout: Int) {
        val uri = URI(serverUri)
        if (socket.isClosed) {
            socket = Socket()
        }
        socket.connect(InetSocketAddress(uri.host, uri.port), timeout)
    }

    override fun breakConnection() {
        outputStream.flush()
        socket.shutdownInput()
        socket.shutdownOutput()
        socket.close()
    }
}