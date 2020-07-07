package com.github.kotlinizer.mqtt

import com.github.kotlinizer.mppktx.coroutines.throwIfCanceled
import com.github.kotlinizer.mqtt.MqttConnectionStatus.*
import com.github.kotlinizer.mqtt.database.MemoryMessageDatabase
import com.github.kotlinizer.mqtt.database.MessageDatabase
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import com.github.kotlinizer.mqtt.internal.connection.packet.received.*
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.*
import com.github.kotlinizer.mqtt.internal.createConnection
import com.github.kotlinizer.mqtt.internal.mqttDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private val connectionStateFlow = MutableStateFlow<MqttConnection?>(null)

    private val connectionMutex = Mutex()

    private val localScope = CoroutineScope(mqttDispatcher + SupervisorJob())

    init {
        localScope.launch(mqttDispatcher) {
            connectionStateFlow
                .filterNotNull()
                .collect { mqttConnection ->
                    try {
                        mqttConnection.packetFlow
                            .collect { packet ->
                                packetReceived(packet)
                            }
                    } catch (t: Throwable) {
                        t.throwIfCanceled()
                        logger?.e(t) {
                            "Error while receiving packet."
                        }
                        errorOccurred()
                    }
                }
        }
        localScope.launch(mqttDispatcher) {
            connectionStateFlow
                .filterNotNull()
                .combine(connectionStatusStateFlow) { connection, status ->
                    connection to status
                }.collectLatest { (connection, status) ->
                    if (status != ESTABLISHING && status != CONNECTED) return@collectLatest
                    connection.packetTransitFlow
                        .collectLatest {
                            delay(connectionConfig.halfKeepAliveMilliseconds)
                            publishPacket(PingReq())
                            delay(connectionConfig.halfKeepAliveMilliseconds)
                            errorOccurred()
                        }
                }
        }
    }

    fun connect() {
        localScope.launch(mqttDispatcher) {
            try {
                connectionMutex.withLock {
                    if (connectionStatus != DISCONNECTED && connectionStatus != ERROR) return@withLock
                    mutableConnectionStatusFlow.value = CONNECTING
                    connectionStateFlow.value = createConnection(connectionConfig, logger).also { connection ->
                        connection.connect()
                    }
                    mutableConnectionStatusFlow.value = ESTABLISHING
                    writePacket(Connect(connectionConfig))
                }
            } catch (t: Throwable) {
                logger?.e(t) { "Unable to connect." }
                errorOccurred()
            }
        }
    }

    suspend fun connect(timeout: Long) {
        withTimeout(timeout) {
            while (connectionStatus != CONNECTED) {
                connect()
                while (connectionStatus == CONNECTING || connectionStatus == ESTABLISHING) {
                    delay(100)
                }
            }
        }
    }

    suspend fun disconnect() {
        try {
            connectionMutex.withLock {
                if (connectionStatus == CONNECTED) {
                    mutableConnectionStatusFlow.value = DISCONNECTING
                    writePacket(Disconnect())
                    connectionStateFlow.value?.disconnectAndClear()
                    connectionStateFlow.value = null
                    mutableConnectionStatusFlow.value = DISCONNECTED
                }
            }
        } catch (t: Throwable) {
            logger?.e(t) { "Error while disconnecting." }
            mutableConnectionStatusFlow.value = ERROR
        }
    }

    fun publishMessage(message: MqttMessage) {
        localScope.launch {
            publishPacket(Publish(message, 0))
        }
    }

    private suspend fun publishPacket(packet: MqttSentPacket) {
        try {
            writePacket(packet)
        } catch (t: Throwable) {
            t.throwIfCanceled()
            logger?.e(t) { "Unable to publish packet: $packet." }
            errorOccurred()
        }
    }

    private suspend fun writePacket(mqttPacket: MqttSentPacket) {
        withContext(NonCancellable) {
            val savedPacket = messageDatabase.savePacket(mqttPacket)
            connectionStateFlow.value?.writePacket(savedPacket)
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
                        logger?.t {
                            "Connection established, now active."
                        }
                        CONNECTED
                    } else {
                        logger?.e(connAck.error) {
                            "Error ConnAck received."
                        }
                        ERROR
                    }
                } else {
                    logger?.e {
                        "Invalid state: Received ConnAck while not in establishing connection."
                    }
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

    private suspend fun errorOccurred() {
        connectionMutex.withLock {
            connectionStateFlow.value?.disconnectAndClear()
            connectionStateFlow.value = null
            mutableConnectionStatusFlow.value = ERROR
        }
    }
}