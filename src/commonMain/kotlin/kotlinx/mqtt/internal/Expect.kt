package kotlinx.mqtt.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.Connection

internal expect fun createConnection(
    connectionConfig: MqttConnectionConfig,
    onConnectionChanged: (Boolean) -> Unit,
    onError: (Exception) -> Unit
): Connection

internal expect val mqttDispatcher: CoroutineDispatcher