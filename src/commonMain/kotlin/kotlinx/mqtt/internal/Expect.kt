package kotlinx.mqtt.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.mqtt.Logger
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.MqttConnection

internal expect fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    onConnectionChanged: (Boolean) -> Unit
): MqttConnection

internal expect val mqttDispatcher: CoroutineDispatcher