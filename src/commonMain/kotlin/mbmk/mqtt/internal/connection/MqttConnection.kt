package mbmk.mqtt.internal.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mbmk.mqtt.Logger
import mbmk.mqtt.MqttConnectionConfig
import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.sent.MqttSentPacket
import mbmk.mqtt.internal.util.changeable

internal abstract class MqttConnection(
    val connectionConfig: MqttConnectionConfig,
    protected val logger: Logger?,
    private val onConnectionChanged: (Boolean) -> Unit
) {

    var connected by changeable(false) { newValue ->
        logger?.d { "Connection state changed. Connected: $newValue." }
        onConnectionChanged(newValue)
    }
        private set

//    abstract val inputStream: Input
//
//    abstract val outputStream: Output

    private val connectionMutex = Mutex()

    suspend fun connect() {
        connectionMutex.withLock {
            if (connected) {
                logger?.t { "Client is already connected." }
                return
            }
            establishConnection(connectionConfig.serverUri, connectionConfig.connectionTimeoutMilliseconds)
            connected = true
        }
    }

    suspend fun disconnect() {
        connectionMutex.withLock {
            if (!connected) {
                logger?.t { "Client is already disconnected." }
                return
            }
            clearConnection()
            connected = false
        }
    }

    /**
     * Sends [packet] to broker.
     */
    abstract suspend fun writePacket(packet: MqttSentPacket)

    /**
     * Waits until new packet is received from broker.
     */
    abstract suspend fun readPacket(): MqttReceivedPacket

    /**
     * @throws Throwable
     */
    protected abstract suspend fun establishConnection(serverUri: String, timeout: Long)

    protected abstract fun clearConnection()
}