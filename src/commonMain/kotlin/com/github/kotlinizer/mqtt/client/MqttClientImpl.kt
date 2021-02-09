package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mppktx.coroutines.throwIfCanceled
import com.github.kotlinizer.mqtt.*
import com.github.kotlinizer.mqtt.MqttConnectionStatus.*
import com.github.kotlinizer.mqtt.database.MemoryMessageDatabase
import com.github.kotlinizer.mqtt.database.MessageDatabase
import com.github.kotlinizer.mqtt.internal.*
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import com.github.kotlinizer.mqtt.internal.connection.packet.received.*
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun MqttClient(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    messageDatabase: MessageDatabase = MemoryMessageDatabase(logger)
): MqttClient {
    return MqttClientImpl(connectionConfig, logger, messageDatabase)
}

private class MqttClientImpl(
    val connectionConfig: MqttConnectionConfig,
    private val logger: Logger?,
    private val messageDatabase: MessageDatabase = MemoryMessageDatabase(logger)
) : MqttClient {

    var connectionStatus: MqttConnectionStatus
        get() = connectionStatusStateFlow.value
        private set(value) {
            connectionStatusStateFlow.value = value
        }

    override val connectionStatusStateFlow by lazy {
        MutableStateFlow(DISCONNECTED)
    }

    private val connectionMutableStateFlow by lazy {
        MutableStateFlow<MqttConnection?>(null)
    }

    private val connectionMutex by lazy {
        Mutex()
    }

    private val localScope by lazy {
        CoroutineScope(mqttDispatcher + SupervisorJob())
    }

    private val subscriptionTracker by lazy {
        SubscriptionTracker()
    }

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

    override suspend fun connect() {
        try {
            connectionMutex.withLock {
                if (connectionStatus != DISCONNECTED && connectionStatus != ERROR) return@withLock
                connectionStatus = CONNECTING
                connectionMutableStateFlow.value = createConnection(connectionConfig, logger).also { connection ->
                    connection.connect()
                }
                connectionStatus = ESTABLISHING
                writePacket(Connect(connectionConfig))
            }
        } catch (t: Throwable) {
            logger?.e(t) { "Unable to connect." }
            errorOccurred()
        }
    }

    override suspend fun disconnect() {
        try {
            connectionMutex.withLock {
                if (connectionStatus == CONNECTED) {
                    connectionStatus = DISCONNECTING
                    writePacket(Disconnect())
                    connectionMutableStateFlow.value?.disconnectAndClear()
                    connectionMutableStateFlow.value = null
                    connectionStatus = DISCONNECTED
                }
            }
        } catch (t: Throwable) {
            logger?.e(t) { "Error while disconnecting." }
            connectionStatus = ERROR
        }
    }

    override suspend fun publishMessage(message: MqttMessage) {
        publishPacket(Publish(0, message))
    }

    override suspend fun subscribe(topic: String, listener: MqttMessageListener) {
        publishPacket(Subscribe(0, mapOf(topic to MqttQos.AT_LEAST_ONCE)))
        subscriptionTracker.addListener(topic, listener)
    }

    private suspend fun publishPacket(packet: MqttSentPacket) {
        try {
            writePacket(packet)
        } catch (t: Throwable) {
            t.throwIfCanceled()
            logger?.e(t) {
                "Unable to publish packet: $packet."
            }
            errorOccurred()
        }
    }

    private suspend fun writePacket(mqttPacket: MqttSentPacket) {
        withContext(NonCancellable) {
            val savedPacket = messageDatabase.savePacket(mqttPacket)
            connectionMutableStateFlow.value?.writePacket(savedPacket)
        }
    }

    private suspend fun packetReceived(mqttReceivedPacket: MqttReceivedPacket) {
        when (mqttReceivedPacket) {
            is ConnAck -> connAckReceived(mqttReceivedPacket)
            is PubAck, is PubComp -> packetCompleted(mqttReceivedPacket)
            is PubRec -> pubRecReceived(mqttReceivedPacket)
            is SubAck -> {

            }
            is Publish -> {
                subscriptionTracker.publishReceived(mqttReceivedPacket)
            }
            else -> logger?.e { "Invalid packet received: $mqttReceivedPacket" }
        }
    }

    private fun connAckReceived(connAck: ConnAck) {
        localScope.launch {
            connectionMutex.withLock {
                connectionStatus = if (connectionStatus == ESTABLISHING) {
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
            connectionStatus = ERROR
        }
    }
}