package kotlinx.milan.mqtt

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed class Connection {

    abstract val connected: Boolean

    private val connectionMutex = Mutex()

    /**
     * @throws Throwable
     */
    suspend fun connect() {
        connectionMutex.withLock {
            if (connected) {
                return
            }
            establishConnection()
        }
    }

    /**
     * @throws Throwable
     */
    abstract fun establishConnection()

}

internal class TestConnection : Connection(){

    override var connected = false

    override fun establishConnection() {
        connected = true
    }
}

//internal class TcpConnection(private val connectionConfig: MqttConnectionConfig) : Connection() {
//
//    private val socketChannel = SocketChannel.open()
//
//    override val connected: Boolean
//        get() = socketChannel.isConnected
//
//    override fun establishConnection() {
//        val socketChanel = SocketChannel.open()
//        socketChanel.connect(InetSocketAddress(connectionConfig.serverUri.host, connectionConfig.serverUri.port))
//    }
//}