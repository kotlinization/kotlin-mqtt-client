package com.github.kotlinizer.mqtt.jvm.sample

import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttConnectionStatus
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.client.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClientPresenter(
    private val presenterScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    val connectionState: StateFlow<MqttConnectionStatus>
        get() = mqttClient.connectionStatusStateFlow

    val logs: StateFlow<List<Log>>
        get() = flowLogger

    private val flowLogger by lazy {
        FlowLogger()
    }

    private val mqttClient by lazy {
        MqttClient(flowLogger)
    }

    fun connectToBroker() {
        presenterScope.launch {
            mqttClient.connect(
                MqttConnectionConfig(serverUri = "tcp://localhost:1883")
            )
        }
    }

    fun disconnectFromBroker() {
        presenterScope.launch {
            mqttClient.disconnect()
        }
    }

    fun subscribeToTopic(topic: String) {
        presenterScope.launch {
            mqttClient.subscribe(topic)
        }
    }

    fun publishMessage(mqttMessage: MqttMessage) {
        presenterScope.launch {
            mqttClient.publishMessage(mqttMessage)
        }
    }

    fun clearLogs() {
        flowLogger.clearLogs()
    }
}