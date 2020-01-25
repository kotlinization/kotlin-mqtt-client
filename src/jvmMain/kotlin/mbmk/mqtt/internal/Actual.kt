package mbmk.mqtt.internal

import kotlinx.coroutines.Dispatchers
import mbmk.mqtt.Logger
import mbmk.mqtt.MqttConnectionConfig
import mbmk.mqtt.internal.connection.MqttConnection

internal actual val mqttDispatcher = Dispatchers.IO

internal actual fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    onConnectionChanged: (Boolean) -> Unit
): MqttConnection {

    return TcpMqttConnection(connectionConfig, logger, onConnectionChanged)
}
