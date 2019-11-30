package kotlinx.mqtt.internal.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.mqtt.Logger
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.packet.ConnAck
import kotlinx.mqtt.internal.connection.packet.Connect
import kotlinx.mqtt.internal.connection.packet.Disconnect
import kotlinx.mqtt.internal.mqttDispatcher
import kotlin.properties.Delegates.observable

internal abstract class Connection(
    private val connectionConfig: MqttConnectionConfig,
    protected val logger: Logger?,
    private val onConnectionChanged: (Boolean) -> Unit
) {

    var connected by observable(false) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            onConnectionChanged(newValue)
        }
    }
        private set

    abstract val inputStream: InputStream

    abstract val outputStream: OutputStream

    private val connectionMutex = Mutex()

    private val packetTracker by lazy { PacketTracker(this) }

    private var receiving: Job? = null

    /**
     * @throws Throwable
     */
    suspend fun connect(): Boolean {
        connectionMutex.withLock {
            if (connected) {
                return true
            }
            establishConnection(connectionConfig.serverUri, connectionConfig.connectionTimeout * 1000)
            packetTracker.writePacket(Connect(connectionConfig)) { received ->
                try {
                    val packet = received as? ConnAck ?: throw IOException("Wrong packet received.")
                    logger?.t { "Packet received: $packet." }
                    packet.error?.let { throw it }
                } catch (io: IOException) {
                    logger?.e(io)
                }
            }
            connected = true
            startReceiving()
            return true
        }
    }

    /**
     * @throws Throwable
     */
    suspend fun disconnect(): Boolean {
        connectionMutex.withLock {
            if (!connected) {
                return true
            }
            connected = false
            stopReceiving()
            packetTracker.writePacket(Disconnect()) {}
            clearConnection()
            return true
        }
    }

    /**
     * @throws Throwable
     */
    protected abstract fun establishConnection(serverUri: String, timeout: Int)

    protected abstract fun clearConnection()

    private fun startReceiving() {
        stopReceiving()
        receiving = GlobalScope.launch(mqttDispatcher) {
            launch(mqttDispatcher) {
                packetTracker.runCatching {
                    while (isActive) {
                        val packet = readPacket()
                        logger?.t { "Packet received: $packet." }
                    }
                }.onFailure {
                    if (connected) {
                        logger?.e(it) { "Error while reading package." }
                        disconnect()
                    }
                }
            }
        }
    }

    private fun stopReceiving() {
        receiving?.cancel()
    }
}