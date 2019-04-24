package org.milan.mqtt

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

internal sealed class Connection {

    abstract val connected: Boolean

    private val connectionMutex = Mutex()

    @Throws(Throwable::class)
    suspend fun connect() {
        connectionMutex.withLock {
            if (connected) {
                return
            }
            establishConnection()
        }
    }

    @Throws(Throwable::class)
    abstract fun establishConnection()

}

internal class TcpConnection(private val connectionConfig: MqttConnectionConfig) : Connection() {

    private val socketChannel = SocketChannel.open()

    override val connected: Boolean
        get() = socketChannel.isConnected

    override fun establishConnection() {
        val socketChanel = SocketChannel.open()
        socketChanel.connect(InetSocketAddress(connectionConfig.serverUri.host, connectionConfig.serverUri.port))
    }
}