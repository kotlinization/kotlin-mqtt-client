package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttConnectionStatus
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.MqttQos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MqttClient {

    val connectionStatusStateFlow: StateFlow<MqttConnectionStatus>

    suspend fun connect(connectionConfig: MqttConnectionConfig)

    suspend fun disconnect()

    suspend fun publishMessage(message: MqttMessage)

    suspend fun subscribe(topic: String, qos: MqttQos = MqttQos.AT_MOST_ONCE): Flow<MqttMessage>
}

val MqttClient.connectionStatus: MqttConnectionStatus
    get() = connectionStatusStateFlow.value