package kotlinx.mqtt.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.mqtt.Logger
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.Connection

internal expect fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger,
    onConnectionChanged: (Boolean) -> Unit
): Connection

internal expect val mqttDispatcher: CoroutineDispatcher