package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mqtt.MqttMessageListener
import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SubscriptionTracker {

    private var listeners = mutableMapOf<String, MutableList<MqttMessageListener>>()

    private val mutex by lazy {
        Mutex()
    }

    suspend fun addListener(topic: String, listener: MqttMessageListener) {
        mutex.withLock {
            listeners.getOrPut(topic) {
                mutableListOf()
            }.add(listener)
        }
    }

    suspend fun publishReceived(publish: Publish) {
        mutex.withLock {
            listeners[publish.mqttMessage.topic]?.forEach {
                it(publish.mqttMessage)
            }
        }
    }

}