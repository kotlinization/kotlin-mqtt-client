package kotlinx.mqtt

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.mqtt.MqttConnectionStatus.*
import kotlinx.mqtt.database.MemoryMessageDatabase
import kotlinx.mqtt.database.MessageDatabase
import kotlinx.mqtt.internal.connection.PacketTracker
import kotlinx.mqtt.internal.connection.packet.Publish
import kotlinx.mqtt.internal.connection.packet.received.ConnAck
import kotlinx.mqtt.internal.connection.packet.sent.Connect
import kotlinx.mqtt.internal.connection.packet.sent.Disconnect
import kotlinx.mqtt.internal.createConnection
import kotlinx.mqtt.internal.mqttDispatcher
import kotlin.properties.Delegates.observable

class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    private val logger: Logger?,
    val messageDatabase: MessageDatabase = MemoryMessageDatabase(),
    onConnectionStatusChanged: (MqttConnectionStatus) -> Unit = {}
) {

    var connectionStatus: MqttConnectionStatus by observable(DISCONNECTED) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            logger?.d { "Connection status changed: $newValue" }
            runCatching {
                onConnectionStatusChanged(newValue)
            }
        }
    }
        private set

    private val connection by lazy {
        createConnection(connectionConfig, logger) {
            GlobalScope.launch(mqttDispatcher) { updateStatus(it) }
        }
    }

    private val packetTracker by lazy {
        PacketTracker(connection, logger) {
            connectionStatus = ERROR
            GlobalScope.launch(mqttDispatcher) {
                updateStatus(connection.connected)
            }
        }
    }

    private val connectionMutex = Mutex()

    suspend fun connect(): Boolean {
        return connectionMutex.withLock {
            try {
                withTimeout(connectionConfig.connectionTimeoutMilliseconds * 2) {
                    if (connectionStatus == CONNECTED) {
                        return@withTimeout true
                    }
                    if (connectionStatus == DISCONNECTED) {
                        connectionStatus = CONNECTING
                        connection.connect()
                    }
                    while (connectionStatus == CONNECTING) {
                        delay(10)
                    }
                    if (connectionStatus != ESTABLISHING) {
                        throw IOException("Unable to establish connection.")
                    }
                    packetTracker.startReceiving()
                    var error: Throwable? = null
                    var finished = false
                    packetTracker.writePacket(Connect(connectionConfig)) { received ->
                        error = try {
                            val packet = received as? ConnAck ?: throw IOException("Wrong packet received.")
                            packet.error
                        } catch (t: Throwable) {
                            t
                        } finally {
                            finished = true
                        }
                    }
                    while (!finished) {
                        delay(10)
                    }
                    error?.let { throw it }
                    logger?.t { "Connection established, now active." }
                    connectionStatus = CONNECTED
                    true
                }
            } catch (t: Throwable) {
                logger?.e(t) { "Unable to connect." }
                connectionStatus = ERROR
                updateStatus(connection.connected)
                false
            }
        }
    }

    suspend fun disconnect() {
        try {
            connectionMutex.withLock {
                if (connectionStatus == CONNECTED) {
                    connectionStatus = DISCONNECTING
                    packetTracker.writePacket(Disconnect()) {}
                    packetTracker.stopReceiving()
                    connection.disconnect()
                    connectionStatus = DISCONNECTED
                }
            }
        } catch (t: Throwable) {
            logger?.e(t) { "Error while disconnecting." }
        }
    }

    suspend fun publish(mqttMessage: MqttMessage): Boolean {
        return withContext(mqttDispatcher) {
            try {
                var completed = false
                when (mqttMessage.constrainedQos) {
                    //Just send message
                    MqttQos.AT_MOST_ONCE -> {
                        packetTracker.writePacket(Publish(mqttMessage))
                        completed = true
                    }
                    MqttQos.AT_LEAST_ONCE -> {
                        val packet = messageDatabase.createPublish(mqttMessage)
                        packetTracker.writePacket(packet) {
                            messageDatabase.messagePublished(it)
                            completed = true
                        }
                    }
                }
                withTimeout(connectionConfig.connectionTimeoutMilliseconds) {
                    while (isActive && !completed) {
                        delay(10)
                    }
                }
                true
            } catch (t: Throwable) {
                logger?.e(t) { "Unable to publish message." }
                false
            }
        }
    }

    private suspend fun updateStatus(connected: Boolean) {
        when (connectionStatus) {
            CONNECTING -> {
                if (connected) {
                    connectionStatus = ESTABLISHING
                }
            }
            ESTABLISHING -> {
                if (!connected) {
                    connectionStatus = DISCONNECTED
                }
            }
            ERROR -> {
                packetTracker.stopReceiving()
                if (connected) {
                    connection.disconnect()
                } else {
                    connectionStatus = DISCONNECTED
                }
            }
            else -> {
                // Do nothing
            }
        }
    }
}