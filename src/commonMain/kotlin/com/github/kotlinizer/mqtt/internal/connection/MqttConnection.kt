package com.github.kotlinizer.mqtt.internal.connection

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.MqttSentPacket
import com.github.kotlinizer.mqtt.internal.util.changeable
import com.github.kotlinizer.mqtt.internal.util.getPacket

internal abstract class MqttConnection(
    val connectionConfig: MqttConnectionConfig,
    protected val logger: Logger?,
    private val onConnectionChanged: (Boolean) -> Unit
) {

    var connected by changeable(false) { newValue ->
        logger?.d { "Connection state changed. Connected: $newValue." }
        onConnectionChanged(newValue)
    }
        private set

    protected abstract val receiveChannel: ReceiveChannel<Byte>

    protected abstract val sendChannel: SendChannel<Byte>

    private val connectionMutex = Mutex()

    private val sendMutex = Mutex()

    private val receiveMutex = Mutex()

    suspend fun connect() {
        connectionMutex.withLock {
            if (connected) {
                logger?.t { "Client is already connected." }
                return
            }
            establishConnection(connectionConfig.serverUri, connectionConfig.connectionTimeoutMilliseconds)
            connected = true
        }
    }

    suspend fun disconnect() {
        connectionMutex.withLock {
            if (!connected) {
                logger?.t { "Client is already disconnected." }
                return
            }
            clearConnection()
            connected = false
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

    /**
     * @throws Throwable
     */
    protected abstract suspend fun establishConnection(serverUri: String, timeout: Long)

    protected abstract fun clearConnection()
}