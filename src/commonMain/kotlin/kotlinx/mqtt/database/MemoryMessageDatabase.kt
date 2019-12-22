package kotlinx.mqtt.database

import kotlinx.mqtt.MqttMessage

class MemoryMessageDatabase : MessageDatabase() {

    override fun saveMessage(mqttMessage: MqttMessage): Short {
        return 1
    }
}

