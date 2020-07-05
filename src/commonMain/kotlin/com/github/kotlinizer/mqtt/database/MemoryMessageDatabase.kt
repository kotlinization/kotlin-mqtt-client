package com.github.kotlinizer.mqtt.database

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttPacket

class MemoryMessageDatabase(logger: Logger?) : MessageDatabase(logger) {

    private val messages = mutableMapOf<Short, MqttPacket>()

    override fun generatePackageId(): Short {
        messages.size.toShort().coerceAtLeast(1)
        var identifier: Short = messages.size.toShort().coerceAtLeast(1)
        while (messages.containsKey(identifier)) {
            identifier++
        }
        return identifier
    }

    override fun storeMessage(mqttPacket: MqttPacket) {
        super.storeMessage(mqttPacket)
        if (messages.containsKey(mqttPacket.packetIdentifier)) {
            logger?.e { "Invalid state, identifier already stored." }
        }
        messages[mqttPacket.packetIdentifier] = mqttPacket
    }

    override fun removeMessage(packetIdentifier: Short) {
        super.removeMessage(packetIdentifier)
        messages.remove(packetIdentifier)
    }
}

