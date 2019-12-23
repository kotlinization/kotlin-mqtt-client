package kotlinx.mqtt.database

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.mqtt.MqttMessage
import kotlinx.mqtt.internal.connection.packet.Publish
import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.received.PubAck

abstract class MessageDatabase {

    private val messageMutex = Mutex()

    internal suspend fun createPublish(mqttMessage: MqttMessage): Publish {
        messageMutex.withLock {
            val id = saveMessage(mqttMessage)
            return Publish(mqttMessage, id)
        }
    }

    internal suspend fun messagePublished(receivedPacket: MqttReceivedPacket) {
        messageMutex.withLock {
            val packageIdentifier = (receivedPacket as? PubAck)?.packageIdentifier ?: return
            deleteMessage(packageIdentifier)
        }
    }

    protected abstract fun saveMessage(mqttMessage: MqttMessage): Short

    protected abstract fun deleteMessage(packageIdentifier: Short)
}