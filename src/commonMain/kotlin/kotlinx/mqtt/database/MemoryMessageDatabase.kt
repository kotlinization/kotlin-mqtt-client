package kotlinx.mqtt.database

import kotlinx.mqtt.MqttMessage

class MemoryMessageDatabase : MessageDatabase() {

    private val messages = mutableMapOf<Short, MqttMessage>()

    override fun saveMessage(mqttMessage: MqttMessage): Short {
        var identifier: Short = messages.size.toShort().coerceAtLeast(1)
        while (messages.containsKey(identifier)) {
            identifier++
        }
        messages[identifier] = mqttMessage
        return identifier
    }

    override fun deleteMessage(packageIdentifier: Short) {
        messages.remove(packageIdentifier)
    }
}

