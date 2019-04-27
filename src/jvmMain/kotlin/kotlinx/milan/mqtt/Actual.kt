package kotlinx.milan.mqtt

import kotlinx.coroutines.Dispatchers
import kotlinx.milan.mqtt.connection.Connection
import kotlinx.milan.mqtt.connection.TcpConnection

internal actual val mqttDispatcher = Dispatchers.IO

internal actual fun createConnection(mqttClientConfig: MqttConnectionConfig): Connection {
    return TcpConnection(mqttClientConfig)
}
