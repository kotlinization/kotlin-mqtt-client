package kotlinx.mqtt.internal.connection

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.packet.Connack
import kotlinx.mqtt.internal.connection.packet.Connect
import kotlinx.mqtt.internal.connection.packet.Disconnect
import kotlinx.mqtt.internal.mqttDispatcher
import kotlin.properties.Delegates.observable

internal abstract class Connection(
    private val connectionConfig: MqttConnectionConfig,
    private val onConnectionChanged: (Boolean) -> Unit,
    private val onError: (Exception) -> Unit
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
                    val packet = received as? Connack ?: throw IOException("Wrong packet received.")
                    packet.error?.let { throw it }
                } catch (io: IOException) {
                    onError(io)
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
                    while (true) {
                        readPacket()
                    }
                }.onFailure {
                    if (connected) {
                        onError(Exception("Error while reading package.", it))
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