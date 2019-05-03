package kotlinx.mqtt

import kotlinx.coroutines.Dispatchers
import kotlinx.mqtt.connection.Connection
import kotlinx.mqtt.connection.TcpConnection

internal actual val mqttDispatcher = Dispatchers.IO

internal actual fun createConnection(onConnectionChanged: (Boolean) -> Unit): Connection {
    return TcpConnection(onConnectionChanged)
}