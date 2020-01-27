package mbmk.mqtt.internal.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import mbmk.mqtt.Logger
import mbmk.mqtt.MqttConnectionConfig
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

    abstract val inputStream: InputStream

    abstract val outputStream: OutputStream

    private val connectionMutex = Mutex()

    /**
     * @throws Throwable
     */
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

    /**
     * @throws Throwable
     */
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
     * @throws Throwable
     */
    protected abstract suspend fun establishConnection(serverUri: String, timeout: Long)

    protected abstract fun clearConnection()
}