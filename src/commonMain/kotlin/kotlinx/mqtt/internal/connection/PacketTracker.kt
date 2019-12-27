package kotlinx.mqtt.internal.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.mqtt.Logger
import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.received.getPacket
import kotlinx.mqtt.internal.connection.packet.sent.MqttSentPacket
import kotlinx.mqtt.internal.connection.packet.sent.PingReq
import kotlinx.mqtt.internal.mqttDispatcher

internal class PacketTracker(
    private val connection: MqttConnection,
    private val logger: Logger?,
    private val onError: () -> Unit
) {

    val packets: ReceiveChannel<MqttReceivedPacket>
        get() = channel

    private val packetsMutex = Mutex()

    private val sentPackets = mutableListOf<PacketResponse>()

    private val channel: Channel<MqttReceivedPacket> = Channel(capacity = RENDEZVOUS)

    private var receivingJob: Job? = null

    private var pingJob: Job? = null

    /**
     * @throws Throwable
     */
    suspend fun writePacket(mqttPacket: MqttSentPacket, onResponse: suspend (MqttReceivedPacket) -> Unit = {}) {
        //In order to prevent response before sent packed is added,
        //packet mutex must be locked.
        packetsMutex.withLock {
            connection.outputStream.write(mqttPacket.pack().toByteArray())
            logger?.t { "Packet written: $mqttPacket." }
            sentPackets.add(PacketResponse(mqttPacket, onResponse))
        }
        packetTransit()
    }

    fun startReceiving() {
        if (receivingJob?.isActive == true) return
        receivingJob = GlobalScope.launch(mqttDispatcher) {
            receivePackets()
        }
    }

    suspend fun stopReceiving() {
        // Lock [packetsMutex] to prevent sending and receiving.
        packetsMutex.withLock {
            receivingJob?.cancelAndJoin()
            pingJob?.cancelAndJoin()
        }
    }

    private suspend fun packetTransit() {
        pingJob?.cancel()
        // Send ping request if no packet has been sent or received in half keep alive interval.
        pingJob = GlobalScope.launch(mqttDispatcher) {
            val timeout = connection.connectionConfig.keepAlive * 500L
            delay(timeout)
            val waitingResponse = GlobalScope.launch(mqttDispatcher) {
                delay(timeout)
                logger?.e { "Ping request timed out" }
                onError()
            }
            GlobalScope.launch {
                writePacket(PingReq()) {
                    waitingResponse.cancel()
                }
            }
        }
    }

    private suspend fun CoroutineScope.receivePackets() {
        try {
            while (isActive) {
                val packet = connection.inputStream.getPacket()
                packetTransit()
                logger?.t { "Packet received: $packet." }
                val responses = packetsMutex.withLock {
                    sentPackets.filter { it.sentPacket.isResponse(packet) }
                }
                if (responses.isEmpty()) {
                    logger?.t { "Adding packet to channel." }
                    channel.send(packet)
                }
                val response = responses.first()
                packetsMutex.withLock {
                    sentPackets.remove(response)
                }
                logger?.t { "Calling packet response." }
                GlobalScope.launch(mqttDispatcher) {
                    response.onResponse(packet)
                }
            }
        } catch (c: CancellationException) {
        } catch (t: Throwable) {
            logger?.e(t) { "Error while receiving packet." }
            onError()
        }
    }
}