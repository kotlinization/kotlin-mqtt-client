package com.github.kotlinizer.mqtt

import com.github.kotlinizer.mppktx.coroutines.throwIfCanceled
import com.github.kotlinizer.mqtt.MqttConnectionStatus.*
import com.github.kotlinizer.mqtt.database.MemoryMessageDatabase
import com.github.kotlinizer.mqtt.database.MessageDatabase
import com.github.kotlinizer.mqtt.internal.PackageReceiver
import com.github.kotlinizer.mqtt.internal.PingRequestTracker
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import com.github.kotlinizer.mqtt.internal.connection.packet.received.*
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.*
import com.github.kotlinizer.mqtt.internal.createConnection
import com.github.kotlinizer.mqtt.internal.mqttDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val connectionMutableStateFlow = MutableStateFlow<MqttConnection?>(null)

    private val connectionMutex = Mutex()

    private val localScope = CoroutineScope(mqttDispatcher + SupervisorJob())

    init {
        PackageReceiver(
            localScope,
            connectionMutableStateFlow,
            logger,
            this::errorOccurred,
            this::packetReceived
        )
        PingRequestTracker(
            localScope,
            connectionConfig,
            connectionMutableStateFlow,
            connectionStatusStateFlow,
            this::errorOccurred
        ) {
            publishPacket(PingReq())
        }
    }

    fun connect() {
        localScope.launch(mqttDispatcher) {
            try {
                connectionMutex.withLock {
                    if (connectionStatus != DISCONNECTED && connectionStatus != ERROR) return@withLock
                    mutableConnectionStatusFlow.value = CONNECTING
                    connectionMutableStateFlow.value = createConnection(connectionConfig, logger).also { connection ->
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
                    connectionMutableStateFlow.value?.disconnectAndClear()
                    connectionMutableStateFlow.value = null
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

    fun subscribe(topic: String) {
        localScope.launch {
            publishPacket(Subscribe(0, mapOf(topic to MqttQos.AT_LEAST_ONCE)))
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
            connectionMutableStateFlow.value?.writePacket(savedPacket)
        }
    }

    private fun packetReceived(mqttReceivedPacket: MqttReceivedPacket) {
        when (mqttReceivedPacket) {
            is ConnAck -> connAckReceived(mqttReceivedPacket)
            is PubAck, is PubComp -> packetCompleted(mqttReceivedPacket)
            is PubRec -> pubRecReceived(mqttReceivedPacket)
            is SubAck -> {
            }
            is Publish ->{
                logger?.t {
                    mqttReceivedPacket.mqttMessage.message.toTypedArray().contentDeepToString()
                }
            }
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
            connectionMutableStateFlow.value?.disconnectAndClear()
            connectionMutableStateFlow.value = null
            mutableConnectionStatusFlow.value = ERROR
        }
    }
}