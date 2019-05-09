package kotlinx.mqtt.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.mqtt.MqttConnectionConfig
import kotlinx.mqtt.internal.connection.Connection

internal actual val mqttDispatcher = Dispatchers.IO

internal actual fun createConnection(
    connectionConfig: MqttConnectionConfig,
    onConnectionChanged: (Boolean) -> Unit,
    onError: (Exception) -> Unit
): Connection {
    return TcpConnection(connectionConfig, onConnectionChanged, onError)
}