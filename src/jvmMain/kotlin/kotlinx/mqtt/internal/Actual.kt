package kotlinx.mqtt.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.mqtt.Logger
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.MqttConnection

internal actual val mqttDispatcher = Dispatchers.IO

internal actual fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    onConnectionChanged: (Boolean) -> Unit
): MqttConnection {
    return TcpMqttConnection(connectionConfig, logger, onConnectionChanged)
}
