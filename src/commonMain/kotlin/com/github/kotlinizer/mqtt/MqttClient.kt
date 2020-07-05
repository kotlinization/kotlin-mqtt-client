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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    private val logger: Logger?,
    private val messageDatabase: MessageDatabase = MemoryMessageDatabase(logger)
) {

    val connectionStatus: MqttConnectionStatus
        get() = connectionStatusStateFlow.value

    val connectionStatusStateFlow: StateFlow<MqttConnectionStatus>
        get() = mutableConnectionStatusFlow

    private val mutableConnectionStatusFlow = MutableStateFlow(DISCONNECTED)

    private val connection by lazy {
        createConnection(connectionConfig, logger)
            .also { mqttConnection ->
                localScope.launch {
                    mqttConnection.connectedStateFlow.collect {
                        updateStatus(it)
                    }
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
                    mutableConnectionStatusFlow.value = ERROR
                    updateStatus(connection.connectedStateFlow.value)
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
                        mutableConnectionStatusFlow.value = CONNECTING
                        connection.connect()
                        mutableConnectionStatusFlow.value = ESTABLISHING
                        packetTracker.startReceiving()
                        packetTracker.writePacket(Connect(connectionConfig))
                    }
                } catch (t: Throwable) {
                    logger?.e(t) { "Unable to connect." }
                    mutableConnectionStatusFlow.value = ERROR
                    updateStatus(connection.connectedStateFlow.value)
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
                    mutableConnectionStatusFlow.value = DISCONNECTING
                    packetTracker.writePacket(Disconnect())
                    packetTracker.stopReceiving()
                    connection.disconnect()
                    mutableConnectionStatusFlow.value = DISCONNECTED
                }
            }
        } catch (t: Throwable) {
            mutableConnectionStatusFlow.value = ERROR
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
            mutableConnectionStatusFlow.value = ERROR
            logger?.e(t) { "Unable to publish packet: $packet." }
        }
    }

    private suspend fun updateStatus(connected: Boolean) {
        when (connectionStatus) {
            CONNECTING -> {
                if (connected) {
                    mutableConnectionStatusFlow.value = ESTABLISHING
                }
            }
            ESTABLISHING -> {
                if (!connected) {
                    mutableConnectionStatusFlow.value = DISCONNECTED
                }
            }
            ERROR -> {
                packetTracker.stopReceiving()
                if (connected) {
                    connection.disconnect()
                } else {
                    mutableConnectionStatusFlow.value = DISCONNECTED
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
                mutableConnectionStatusFlow.value = if (connectionStatus == ESTABLISHING) {
                    if (connAck.error == null) {
                        logger?.t { "Connection established, now active." }
                        CONNECTED
                    } else {
                        logger?.e(connAck.error) { "Error ConnAck received." }
                        ERROR
                    }
                } else {
                    logger?.e { "Invalid state: Received ConnAck while not in establishing connection." }
                    ERROR
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