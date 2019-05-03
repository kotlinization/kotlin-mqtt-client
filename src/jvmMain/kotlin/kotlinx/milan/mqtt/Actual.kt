package kotlinx.milan.mqtt

import kotlinx.coroutines.Dispatchers
import kotlinx.milan.mqtt.connection.Connection
import kotlinx.milan.mqtt.connection.TcpConnection

internal actual val mqttDispatcher = Dispatchers.IO

internal actual fun createConnection(onConnectionChanged: (Boolean) -> Unit): Connection {
    return TcpConnection(onConnectionChanged)
}