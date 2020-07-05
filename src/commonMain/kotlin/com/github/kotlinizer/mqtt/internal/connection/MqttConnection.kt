package com.github.kotlinizer.mqtt.internal.connection

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.MqttSentPacket
import com.github.kotlinizer.mqtt.internal.util.getPacket
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class MqttConnection(
    val connectionConfig: MqttConnectionConfig,
    protected val logger: Logger?
) {

    val connectedStateFlow: StateFlow<Boolean>
        get() = mutableConnectedFlow

    protected abstract val receiveChannel: ReceiveChannel<Byte>

    protected abstract val sendChannel: SendChannel<Byte>

    private val connectionMutex = Mutex()

    private val sendMutex = Mutex()

    private val mutableConnectedFlow = MutableStateFlow(false)

    private val receiveMutex = Mutex()

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
                sendChannel.send(it)
            }
        }
    }

    /**
     * Waits until new packet is received from broker.
     */
    suspend fun readPacket(): MqttReceivedPacket {
        return receiveMutex.withLock {
            receiveChannel.getPacket()
        }
    }

    protected fun connectionBroken() {
        clearConnection()
        mutableConnectedFlow.value = false
    }

    protected abstract suspend fun establishConnection(serverUri: String, timeout: Long)

    protected abstract fun clearConnection()
}