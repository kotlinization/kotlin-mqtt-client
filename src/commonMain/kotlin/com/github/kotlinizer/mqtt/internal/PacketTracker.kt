package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mppktx.coroutines.throwIfCanceled
import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.database.MessageDatabase
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.received.PingResp
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.MqttSentPacket
import com.github.kotlinizer.mqtt.internal.connection.packet.sent.PingReq
import kotlinx.coroutines.*

internal class PacketTracker(
    private val connection: MqttConnection,
    private val messageDatabase: MessageDatabase,
    private val logger: Logger?,
    private val onPacketReceived: (MqttReceivedPacket) -> Unit,
    private val onError: () -> Unit
) {

    private var receivingJob: Job? = null

    private var pingJob: Job? = null

    private val localScope = CoroutineScope(mqttDispatcher + SupervisorJob())

    suspend fun writePacket(mqttPacket: MqttSentPacket): MqttSentPacket {
        val savedPacket: MqttSentPacket = messageDatabase.savePacket(mqttPacket)
        return withContext(NonCancellable) {
            connection.writePacket(savedPacket)
            logger?.t { "Packet written: $savedPacket." }
            packetTransit()
            savedPacket
        }
    }

    fun startReceiving() {
        if (receivingJob?.isActive == true) return
        receivingJob = localScope.launch {
            receivePackets()
        }
    }

    suspend fun stopReceiving() {
        receivingJob?.cancelAndJoin()
        pingJob?.cancelAndJoin()
    }

    private suspend fun packetTransit() {
        pingJob?.cancel()
        // Send ping request if no packet has been sent or received in half keep alive interval.
        pingJob = localScope.launch {
            val timeout = connection.connectionConfig.keepAlive * 500L
            delay(timeout)
            runCatching {
                writePacket(PingReq())
            }.onFailure {
                onError()
            }
            delay(timeout)
            if (isActive) {
                logger?.e { "Ping request timed out" }
                onError()
            }
        }
    }

    private suspend fun receivePackets() {
        withContext(mqttDispatcher) {
            try {
                while (isActive) {
                    val packet = connection.readPacket()
                    packetTransit()
                    logger?.t { "Packet received: $packet." }
                    if (packet !is PingResp) {
                        onPacketReceived(packet)
                    }
                }
            } catch (t: Throwable) {
                t.throwIfCanceled()
                logger?.e(t) { "Error while receiving packet." }
                onError()
            }
        }
    }
}