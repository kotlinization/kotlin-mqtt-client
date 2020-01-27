package mbmk.mqtt.internal

import kotlinx.coroutines.*
import mbmk.mqtt.Logger
import mbmk.mqtt.database.MessageDatabase
import mbmk.mqtt.internal.connection.MqttConnection
import mbmk.mqtt.internal.connection.packet.received.MqttReceivedPacket
import mbmk.mqtt.internal.connection.packet.received.PingResp
import mbmk.mqtt.internal.connection.packet.received.getPacket
import mbmk.mqtt.internal.connection.packet.sent.MqttSentPacket
import mbmk.mqtt.internal.connection.packet.sent.PingReq
import mbmk.mqtt.internal.util.throwIfCancel

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

    /**
     * @throws Throwable
     */
    suspend fun writePacket(mqttPacket: MqttSentPacket): MqttSentPacket {
        val savedPacket: MqttSentPacket = messageDatabase.savePacket(mqttPacket)
        return withContext(NonCancellable) {
            connection.outputStream.write(savedPacket.pack().toByteArray())
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
                    val packet = connection.inputStream.getPacket()
                    packetTransit()
                    logger?.t { "Packet received: $packet." }
                    if (packet !is PingResp) {
                        onPacketReceived(packet)
                    }
                }
            } catch (t: Throwable) {
                t.throwIfCancel()
                logger?.e(t) { "Error while receiving packet." }
                onError()
            }
        }
    }
}