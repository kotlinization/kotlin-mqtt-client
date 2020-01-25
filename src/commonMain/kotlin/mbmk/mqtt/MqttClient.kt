package mbmk.mqtt

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import mbmk.mqtt.MqttConnectionStatus.*
import mbmk.mqtt.database.MemoryMessageDatabase
import mbmk.mqtt.database.MessageDatabase
import mbmk.mqtt.internal.changeable
import mbmk.mqtt.internal.connection.PacketTracker
import mbmk.mqtt.internal.connection.packet.Publish
import mbmk.mqtt.internal.connection.packet.received.ConnAck
import mbmk.mqtt.internal.connection.packet.sent.Connect
import mbmk.mqtt.internal.connection.packet.sent.Disconnect
import mbmk.mqtt.internal.connection.packet.sent.PubRel
import mbmk.mqtt.internal.createConnection
import mbmk.mqtt.internal.mqttDispatcher

class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    private val logger: Logger?,
    private val messageDatabase: MessageDatabase = MemoryMessageDatabase(),
    onConnectionStatusChanged: (MqttConnectionStatus) -> Unit = {}
) {

    var connectionStatus: MqttConnectionStatus by changeable(DISCONNECTED) { newValue ->
        logger?.d { "Connection status changed: $newValue" }
        runCatching {
            onConnectionStatusChanged(newValue)
        }
    }
        private set

    private val connection by lazy {
        createConnection(connectionConfig, logger) {
            localScope.launch { updateStatus(it) }
        }
    }

    private val packetTracker by lazy {
        PacketTracker(connection, logger) {
            connectionStatus = ERROR
            localScope.launch {
                updateStatus(connection.connected)
            }
        }
    }

    private val connectionMutex = Mutex()

    private val localScope = CoroutineScope(mqttDispatcher + SupervisorJob())

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
                    MqttQos.EXACTLY_ONCE -> {
                        val packet = messageDatabase.createPublish(mqttMessage)
                        val packetIdentifier = packet.packetIdentifier ?: throw IllegalStateException("Packet identifier must not be null.")
                        packetTracker.writePacket(packet) {
                            packetTracker.writePacket(PubRel(packetIdentifier)) {
                                messageDatabase.messagePublished(it)
                                completed = true
                            }
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