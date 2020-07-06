package com.github.kotlinizer.mqtt.internal.connection

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.received.PingResp
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.MqttSentPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.PingReq
import com.github.kotlinizer.mqtt.internal.util.getPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class MqttConnection(
    private val connectionConfig: MqttConnectionConfig,
    protected val logger: Logger?
) {

    val errorFlow: Flow<String>
        get() = channelFlow {
            var pingJob = createPingJob(this)
            var packetTrackerJob: Job? = null
            connectedStateFlow.collect { connected ->
                packetTrackerJob?.cancel()
                if (connected) {
                    packetTrackerJob = launch {
                        while (isActive) {
                            packetSentChannel.receive()
                            pingJob.cancelAndJoin()
                            pingJob = createPingJob(this@channelFlow)
                        }
                    }
                } else {
                    pingJob.cancelAndJoin()
                }
            }
        }

    val packetFlow: Flow<MqttReceivedPacket>
        get() = flow {
            while (true) {
                val packet = readPacket()
                logger?.t { "Packet received: $packet." }
                if (packet !is PingResp) {
                    emit(packet)
                }
            }
        }

    val connectedStateFlow: StateFlow<Boolean>
        get() = mutableConnectedFlow

    protected val clientToBrokerChannel: ReceiveChannel<Byte>
        get() = sendingChannel

    protected val brokerToClientChannel: SendChannel<Byte>
        get() = receivingChannel

    private val connectionMutex = Mutex()

    private val sendMutex = Mutex()

    private val mutableConnectedFlow = MutableStateFlow(false)

    private val receiveMutex = Mutex()

    private val sendingChannel = Channel<Byte>(Channel.RENDEZVOUS)

    private val receivingChannel = Channel<Byte>(Channel.RENDEZVOUS)

    private val packetSentChannel = Channel<Unit>(Channel.RENDEZVOUS)

    suspend fun connect() {
        connectionMutex.withLock {
            if (mutableConnectedFlow.value) {
                logger?.t { "Client is already connected." }
                return
            }
            establishConnection(connectionConfig.serverUri, connectionConfig.connectionTimeoutMilliseconds)
            mutableConnectedFlow.value = true
        }
    }

    suspend fun disconnect() {
        connectionMutex.withLock {
            if (!mutableConnectedFlow.value) {
                logger?.t { "Client is already disconnected." }
                return
            }
            clearConnection()
            mutableConnectedFlow.value = false
        }
    }

    /**
     * Sends [packet] to broker.
     */
    suspend fun writePacket(packet: MqttSentPacket) {
        sendMutex.withLock {
            packet.pack().forEach {
                sendingChannel.send(it)
            }
        }
        logger?.t { "Packet written: $packet." }
        packetSentChannel.send(Unit)
    }

    /**
     * Waits until new packet is received from broker.
     */
    suspend fun readPacket(): MqttReceivedPacket {
        return receiveMutex.withLock {
            receivingChannel.getPacket()
        }.also {
            packetSentChannel.send(Unit)
        }
    }

    protected fun connectionBroken() {
        clearConnection()
        mutableConnectedFlow.value = false
    }

    protected abstract suspend fun establishConnection(serverUri: String, timeout: Long)

    protected abstract fun clearConnection()

    private fun createPingJob(producerScope: ProducerScope<String>): Job {
        return producerScope.launch {
            val timeout = connectionConfig.keepAlive * 500L
            delay(timeout)
            writePacket(PingReq())
            delay(timeout)
            if (isActive) {
                producerScope.send("Ping request timed out.")
            }
        }
    }
}