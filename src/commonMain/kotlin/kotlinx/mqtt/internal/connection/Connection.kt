package kotlinx.mqtt.internal.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.mqtt.Logger
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.packet.received.ConnAck
import kotlinx.mqtt.internal.connection.packet.sent.Connect
import kotlinx.mqtt.internal.connection.packet.sent.Disconnect
import kotlinx.mqtt.internal.mqttDispatcher
import kotlin.properties.Delegates.observable

internal abstract class Connection(
    private val connectionConfig: MqttConnectionConfig,
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

    private val packetTracker by lazy { PacketTracker(this, logger) }

    private var receiving: Job? = null

    /**
     * @throws Throwable
     */
    suspend fun connect(): Boolean {
        connectionMutex.withLock {
            if (connected) {
                logger?.t { "Client is already connected, returning true." }
                return true
            }
            val timeout = connectionConfig.connectionTimeout * 1000L
            withTimeout(timeout) {
                establishConnection(connectionConfig.serverUri, timeout)
                var finished = false
                packetTracker.writePacket(Connect(connectionConfig)) { received ->
                    try {
                        val packet = received as? ConnAck ?: throw IOException("Wrong packet received.")
                        packet.error?.let { throw it }
                        logger?.t { "Connection acknowledged, now active." }
                        connected = true
                        startReceiving()
                    } catch (t: Throwable) {
                        logger?.e(t)
                    } finally {
                        finished = true
                    }
                }
                while (!finished) {
                    delay(10)
                }
            }
            logger?.t { "Connection finished, client is connected: $connected." }
            return connected
        }
    }

    /**
     * @throws Throwable
     */
    suspend fun disconnect(): Boolean {
        connectionMutex.withLock {
            if (!connected) {
                logger?.t { "Client is already disconnected, returning true." }
                return true
            }
            connected = false
            stopReceiving()
            packetTracker.writePacket(Disconnect()) {}
            clearConnection()
            logger?.t { "Client is disconnected." }
            return true
        }
    }

    /**
     * @throws Throwable
     */
    protected abstract fun establishConnection(serverUri: String, timeout: Long)

    protected abstract fun clearConnection()

    private fun startReceiving() {
        stopReceiving()
        receiving = GlobalScope.launch(mqttDispatcher) {
            //            launch(mqttDispatcher) {
//                packetTracker.runCatching {
//                    while (isActive) {
//                        val packet = readPacket()
//                    }
//                }.onFailure {
//                    if (connected) {
//                        logger?.e(it) { "Error while reading package." }
//                        disconnect()
//                    }
//                }
//            }
        }
    }

    private fun stopReceiving() {
        receiving?.cancel()
    }
}