package kotlinx.mqtt.internal.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.mqtt.Logger
import kotlinx.mqtt.MqttConnectionConfig
import kotlin.properties.Delegates.observable

internal abstract class MqttConnection(
    val connectionConfig: MqttConnectionConfig,
    protected val logger: Logger?,
    private val onConnectionChanged: (Boolean) -> Unit
) {

    var connected by observable(false) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            logger?.d { "Connection state changed. Connected: $newValue." }
            onConnectionChanged(newValue)
        }
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
                logger?.t { "Client is already connected, returning true." }
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