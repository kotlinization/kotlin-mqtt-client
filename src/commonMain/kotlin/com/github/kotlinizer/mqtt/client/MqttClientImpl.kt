package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import com.github.kotlinizer.mqtt.MqttConnectionStatus.*
import com.github.kotlinizer.mqtt.database.MemoryMessageDatabase
import com.github.kotlinizer.mqtt.database.MessageDatabase
import com.github.kotlinizer.mqtt.internal.*
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import com.github.kotlinizer.mqtt.internal.connection.packet.received.*
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.*
import com.github.kotlinizer.mqtt.internal.util.createPacketFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun MqttClient(
    logger: Logger? = null,
    messageDatabase: MessageDatabase = MemoryMessageDatabase(logger),
): MqttClient {
    return MqttClientImpl(logger, messageDatabase)
}

private class MqttClientImpl(
    private val logger: Logger?,
    private val messageDatabase: MessageDatabase,
) : MqttClient {

    private var receivingJob: Job? = null

    var connectionStatus: MqttConnectionStatus
        get() = connectionStatusStateFlow.value
        private set(value) {
            if (connectionStatus == value) return
            connectionStatusStateFlow.value = value
            if (value == CONNECTED) {
                receivingJob = localScope.launch(Dispatchers.MqttDispatcher) {
                    packetSharedFlow.collect(::packetReceived)
                }
            } else {
                receivingJob?.cancel()
                pingRequestTracker.stopPinging()
            }
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
        CoroutineScope(Dispatchers.MqttDispatcher + SupervisorJob())
    }

    private val packetSharedFlow by lazy {
        connectionMutableStateFlow.createPacketFlow(localScope)
    }

    private val pingRequestTracker by lazy {
        PingRequestTracker(
            localScope = localScope,
            logger = logger,
            connectionFlow = connectionMutableStateFlow,
            errorOccurred = this::errorOccurred
        ) {
            publishPacket(PingReq())
        }
    }

    override suspend fun connect(connectionConfig: MqttConnectionConfig) {
        try {
            connectionMutex.withLock {
                if (connectionStatus != DISCONNECTED && connectionStatus != ERROR) return@withLock

                connectionStatus = CONNECTING
                connectionMutableStateFlow.value = createConnection(connectionConfig, logger)
                    .also { connection -> connection.connect() }

                connectionStatus = ESTABLISHING
                writePacket(Connect(connectionConfig))
                val connAck = packetSharedFlow
                    .filterIsInstance<ConnAck>()
                    .first()

                connectionStatus = if (connAck.error == null) {
                    logger?.t { "Connection established, now active." }
                    pingRequestTracker.startPinging(connectionConfig)
                    CONNECTED
                } else {
                    logger?.e(connAck.error) { "Error ConnAck received." }
                    ERROR
                }
            }
        } catch (e: Exception) {
            logger?.e(e) { "Unable to connect." }
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

    override suspend fun subscribe(topic: String, qos: MqttQos): Flow<MqttMessage> {
        publishPacket(Subscribe(0, mapOf(topic to MqttQos.AT_LEAST_ONCE)))
        return channelFlow {
            packetSharedFlow.filterIsInstance<Publish>()
                .filter {
                    // TODO Add topic matcher
                    it.mqttMessage.topic == topic
                }.collect {
                    send(it.mqttMessage)
                }
        }
    }

    private suspend fun publishPacket(packet: MqttSentPacket) {
        try {
            writePacket(packet)
        } catch (t: Throwable) {
            logger?.e(t) { "Unable to publish packet: $packet." }
            errorOccurred()
        }
    }

    private suspend fun writePacket(mqttPacket: MqttSentPacket) {
        val savedPacket = messageDatabase.savePacket(mqttPacket)
        connectionMutableStateFlow.value?.writePacket(savedPacket)
    }

    private fun packetReceived(mqttReceivedPacket: MqttReceivedPacket) {
        when (mqttReceivedPacket) {
            is ConnAck -> connAckReceived()
            is PubAck, is PubComp -> packetCompleted(mqttReceivedPacket)
            is PubRec -> pubRecReceived(mqttReceivedPacket)
        }
    }

    private fun connAckReceived() {
        localScope.launch {
            connectionMutex.withLock {
                logger?.e { "Invalid state: Received ConnAck while not in establishing connection." }
                connectionStatus = ERROR
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
        logger?.e { "Ping response NOT received." }
        connectionMutex.withLock {
            connectionMutableStateFlow.value?.disconnectAndClear()
            connectionMutableStateFlow.value = null
            connectionStatus = ERROR
        }
    }
}