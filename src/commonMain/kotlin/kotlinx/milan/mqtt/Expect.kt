package kotlinx.milan.mqtt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.milan.mqtt.connection.Connection

internal expect fun createConnection(mqttClientConfig: MqttConnectionConfig): Connection

internal expect val mqttDispatcher: CoroutineDispatcher