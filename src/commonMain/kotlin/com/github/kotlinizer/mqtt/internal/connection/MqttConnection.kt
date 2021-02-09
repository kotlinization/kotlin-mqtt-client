package com.github.kotlinizer.mqtt.internal.connection

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.received.PingResp
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.MqttSentPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.PingReq
import com.github.kotlinizer.mqtt.internal.util.getPacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class MqttConnection(
    protected val connectionConfig: MqttConnectionConfig,
    protected val logger: Logger?
) {

    val packetTransitFlow: Flow<Unit>
        get() = packetTransitSharedFlow

    val packetFlow: Flow<MqttReceivedPacket> = channelFlow {
        val channel = Channel<Byte>(Channel.RENDEZVOUS)
        launch {
            brokerToClientFlow.collect {
                channel.send(it)
            }
        }
        launch {
            while (isActive) {
                val packet = channel.getPacket().also {
                    packetTransitSharedFlow.emit(Unit)
                }
                logger?.t { "Packet received: $packet." }
                if (packet !is PingResp) send(packet)
            }
        }
    }

    protected abstract val brokerToClientFlow: Flow<Byte>

    private val sendMutex = Mutex()

    private val packetTransitSharedFlow = MutableSharedFlow<Unit>()

    abstract suspend fun connect()

    abstract suspend fun disconnectAndClear()

    /**
     * Sends [packet] to broker.
     */
    suspend fun writePacket(packet: MqttSentPacket) {
        sendMutex.withLock {
            writeBytes(packet.pack())
        }
        logger?.t { "Packet written: $packet." }
        if (packet !is PingReq) {
            packetTransitSharedFlow.emit(Unit)
        }
    }

    protected abstract suspend fun writeBytes(bytes: List<Byte>)
}