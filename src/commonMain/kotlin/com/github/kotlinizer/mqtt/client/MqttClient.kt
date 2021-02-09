package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.MqttConnectionStatus
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.MqttMessageListener
import kotlinx.coroutines.flow.StateFlow

interface MqttClient {

    val connectionStatusStateFlow: StateFlow<MqttConnectionStatus>

    suspend fun connect()

    suspend fun disconnect()

    suspend fun publishMessage(message: MqttMessage)

    suspend fun subscribe(topic: String, listener: MqttMessageListener)
}

val MqttClient.connectionStatus: MqttConnectionStatus
    get() = connectionStatusStateFlow.value