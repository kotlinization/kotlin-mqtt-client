package kotlinx.mqtt.internal.connection

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.mqtt.internal.connection.packet.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.MqttSentPacket
import kotlinx.mqtt.internal.connection.packet.getPacket
import kotlinx.mqtt.internal.mqttDispatcher

internal class PacketTracker(private val connection: Connection) {

    private val sendingMutex = Mutex()

    private val receivingMutex = Mutex()

    private val packetsMutex = Mutex()

    private val sentPackets = mutableListOf<PacketResponse>()

    /**
     * @throws Throwable
     */
    suspend fun writePacket(mqttPacket: MqttSentPacket, onResponse: suspend (MqttReceivedPacket) -> Unit) {
        sendingMutex.withLock {
            connection.outputStream.write(mqttPacket.pack().toByteArray())
            sentPackets.withLock { add(PacketResponse(mqttPacket, onResponse)) }
        }
    }

    /**
     * @throws Throwable
     */
    suspend fun readPacket(): MqttReceivedPacket {
        return receivingMutex.withLock { receiving() }
    }

    /**
     * @throws Throwable
     */
    private suspend fun receiving(): MqttReceivedPacket {
        while (true) {
            val packet = connection.inputStream.getPacket()
            val responses = sentPackets.withLock {
                filter { it.sentPacket.isResponse(packet) }
            }
            if (responses.isEmpty()) {
                return packet
            }
            val response = responses.singleOrNull() ?: throw IOException("Internal error with packet tracking.")
            GlobalScope.launch(mqttDispatcher) { response.onResponse(packet) }
        }
    }

    private suspend fun <E, R> MutableList<E>.withLock(block: suspend MutableList<E>.() -> R): R {
        return packetsMutex.withLock {
            block(this)
        }
    }
}