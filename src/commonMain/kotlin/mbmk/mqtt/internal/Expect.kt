package mbmk.mqtt.internal

import kotlinx.coroutines.CoroutineDispatcher
import mbmk.mqtt.Logger
import mbmk.mqtt.MqttConnectionConfig
import mbmk.mqtt.internal.connection.MqttConnection

internal expect fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    onConnectionChanged: (Boolean) -> Unit
): MqttConnection

internal expect val mqttDispatcher: CoroutineDispatcher