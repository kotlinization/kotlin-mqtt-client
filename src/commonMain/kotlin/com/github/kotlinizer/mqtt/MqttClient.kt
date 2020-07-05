package com.github.kotlinizer.mqtt

import com.github.kotlinizer.mqtt.MqttConnectionStatus.*
import com.github.kotlinizer.mqtt.database.MemoryMessageDatabase
import com.github.kotlinizer.mqtt.database.MessageDatabase
import com.github.kotlinizer.mqtt.internal.PacketTracker
import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import com.github.kotlinizer.mqtt.internal.connection.packet.received.*
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.Connect
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.Disconnect
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.MqttSentPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.PubRel
import com.github.kotlinizer.mqtt.internal.createConnection
import com.github.kotlinizer.mqtt.internal.mqttDispatcher
import com.github.kotlinizer.mqtt.internal.util.changeable
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    private val logger: Logger?,
    private val messageDatabase: MessageDatabase = MemoryMessageDatabase(logger),
    onConnectionStatusChanged: (MqttConnectionStatus) -> Unit = {}
) {

    var connectionStatus: MqttConnectionStatus by changeable(DISCONNECTED) { newValue ->
        logger?.t { "Connection status changed: $newValue" }
        runCatching {
            onConnectionStatusChanged(newValue)
        }.onFailure {
            logger?.e(it) { "Error while invoking callback." }
        }
    }
        private set

    private val connection by lazy {
        createConnection(connectionConfig, logger) {
            localScope.launch {
                updateStatus(it)
            }
        }
    }

    private val packetTracker by lazy {
        PacketTracker(
            connection, messageDatabase, logger,
            onError = this::onPacketTrackerError,
            onPacketReceived = this::packetReceived
        )
    }

    private fun onPacketTrackerError() {
        localScope.launch {
            connectionMutex.withLock {
                if (connectionStatus == CONNECTED || connectionStatus == CONNECTING || connectionStatus == ESTABLISHING) {
                    connectionStatus = ERROR
                    updateStatus(connection.connected)
                }
            }
        }
    }

    private val connectionMutex = Mutex()

    private val localScope = CoroutineScope(mqttDispatcher + SupervisorJob())

    fun connect() {
        localScope.launch {
            connectionMutex.withLock {
                try {
                    if (connectionStatus == DISCONNECTED || connectionStatus == ERROR) {
                        connectionStatus = CONNECTING
                        connection.connect()
                        connectionStatus = ESTABLISHING
                        packetTracker.startReceiving()
                        packetTracker.writePacket(Connect(connectionConfig))
                    }
                } catch (t: Throwable) {
                    logger?.e(t) { "Unable to connect." }
                    connectionStatus = ERROR
                    updateStatus(connection.connected)
                }
            }
        }
    }

    suspend fun connect(timeout: Long) {
        withTimeout(timeout) {
            while (connectionStatus != CONNECTED) {
                connect()
                while (connectionStatus == CONNECTING || connectionStatus == ESTABLISHING) {
                    delay(10)
                }
            }
        }
    }

    suspend fun disconnect() {
        try {
            connectionMutex.withLock {
                if (connectionStatus == CONNECTED) {
                    connectionStatus = DISCONNECTING
                    packetTracker.writePacket(Disconnect())
                    packetTracker.stopReceiving()
                    connection.disconnect()
                    connectionStatus = DISCONNECTED
                }
            }
        } catch (t: Throwable) {
            connectionStatus = ERROR
            logger?.e(t) { "Error while disconnecting." }
        }
    }

    fun publishMessage(message: MqttMessage) {
        localScope.launch {
            publishPacket(Publish(message, 0))
        }
    }

    private suspend fun publishPacket(packet: MqttSentPacket) {
        try {
            packetTracker.writePacket(packet)
        } catch (t: Throwable) {
            connectionStatus = ERROR
            logger?.e(t) { "Unable to publish packet: $packet." }
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

    private fun packetReceived(mqttReceivedPacket: MqttReceivedPacket) {
        when (mqttReceivedPacket) {
            is ConnAck -> connAckReceived(mqttReceivedPacket)
            is PubAck, is PubComp -> packetCompleted(mqttReceivedPacket)
            is PubRec -> pubRecReceived(mqttReceivedPacket)
            else -> logger?.e { "Invalid packet received: $mqttReceivedPacket" }
        }
    }

    private fun connAckReceived(connAck: ConnAck) {
        localScope.launch {
            connectionMutex.withLock {
                if (connectionStatus == ESTABLISHING) {
                    if (connAck.error == null) {
                        connectionStatus = CONNECTED
                        logger?.t { "Connection established, now active." }
                    } else {
                        connectionStatus = ERROR
                        logger?.e(connAck.error) { "Error ConnAck received." }
                    }
                } else {
                    connectionStatus = ERROR
                    logger?.e { "Invalid state: Received ConnAck while not in establishing connection." }
                }
            }
        }
    }

    private fun packetCompleted(mqttReceivedPacket: MqttReceivedPacket) {
        localScope.launch {
            messageDatabase.deletePacket(mqttReceivedPacket.packetIdentifier)
        }
    }

    private fun pubRecReceived(pubRec: PubRec) {
        localScope.launch {
            publishPacket(PubRel(pubRec.packetIdentifier))
        }
    }
}