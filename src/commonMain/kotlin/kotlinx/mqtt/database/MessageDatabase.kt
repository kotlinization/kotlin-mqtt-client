package kotlinx.mqtt.database

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.mqtt.MqttMessage
import kotlinx.mqtt.internal.connection.packet.Publish

abstract class MessageDatabase {

    private val messageMutex = Mutex()

    internal suspend fun createPublish(mqttMessage: MqttMessage): Publish {
        messageMutex.withLock {
            val id = saveMessage(mqttMessage)
            return Publish(mqttMessage, id)
        }
    }

    protected abstract fun saveMessage(mqttMessage: MqttMessage): Short
}