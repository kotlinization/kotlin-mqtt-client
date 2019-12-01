package kotlinx.mqtt.internal.connection

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.mqtt.Logger
import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.received.getPacket
import kotlinx.mqtt.internal.connection.packet.sent.MqttSentPacket
import kotlinx.mqtt.internal.mqttDispatcher

internal class PacketTracker(private val connection: Connection, private val logger: Logger?) {

    val packets: ReceiveChannel<MqttReceivedPacket>
        get() = channel

    private val packetsMutex = Mutex()

    private val sentPackets = mutableListOf<PacketResponse>()

    private val channel: Channel<MqttReceivedPacket> = Channel(capacity = RENDEZVOUS)

    init {
        receivePackets()
    }

    /**
     * @throws Throwable
     */
    suspend fun writePacket(mqttPacket: MqttSentPacket, onResponse: suspend (MqttReceivedPacket) -> Unit) {
        packetsMutex.withLock {
            connection.outputStream.write(mqttPacket.pack().toByteArray())
            logger?.t { "Packet written: $mqttPacket." }
            sentPackets.add(PacketResponse(mqttPacket, onResponse))
        }
    }

    private fun receivePackets() {
        GlobalScope.launch {
            while (isActive) {
                try {
                    val packet = connection.inputStream.getPacket()
                    logger?.t { "Packet received: $packet." }
                    val responses = packetsMutex.withLock {
                        sentPackets.filter { it.sentPacket.isResponse(packet) }
                    }
                    if (responses.isEmpty()) {
                        logger?.t { "Adding packet to channel." }
                        channel.send(packet)
                    }
                    val response = responses.singleOrNull() ?: throw IOException("Internal error with packet tracking.")
                    logger?.t { "Calling packet response." }
                    GlobalScope.launch(mqttDispatcher) {
                        response.onResponse(packet)
                    }
                } catch (t: Throwable) {
                    logger?.e(t) { "Error while receiving packet." }
                }
            }
        }
    }
}