package kotlinx.mqtt.database

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.mqtt.MqttMessage
import kotlinx.mqtt.internal.connection.packet.Publish
import kotlinx.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.mqtt.internal.connection.packet.received.PubAck
import kotlinx.mqtt.internal.connection.packet.received.PubComp

abstract class MessageDatabase {

    private val messageMutex = Mutex()

    internal suspend fun createPublish(mqttMessage: MqttMessage): Publish {
        messageMutex.withLock {
            val id = saveMessage(mqttMessage)
            return Publish(mqttMessage, id)
        }
    }

    internal suspend fun messagePublished(receivedPacket: MqttReceivedPacket) {
        when (receivedPacket) {
            is PubAck -> {
                messageMutex.withLock { deleteMessage(receivedPacket.packageIdentifier) }
            }
            is PubComp -> {
                messageMutex.withLock { deleteMessage(receivedPacket.packageIdentifier) }
            }
        }
    }

    protected abstract fun saveMessage(mqttMessage: MqttMessage): Short

    protected abstract fun deleteMessage(packageIdentifier: Short)
}