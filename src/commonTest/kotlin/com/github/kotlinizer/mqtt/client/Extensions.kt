package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.MqttConnectionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

suspend fun MqttClient.connect(timeout: Long){
    withTimeout(timeout) {
        while (connectionStatus != MqttConnectionStatus.CONNECTED) {
            connect()
            while (connectionStatus == MqttConnectionStatus.CONNECTING || connectionStatus == MqttConnectionStatus.ESTABLISHING) {
                delay(100)
            }
        }
    }
}