package kotlinx.milan.mqtt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.milan.mqtt.connection.Connection

internal expect fun createConnection(onConnectionChanged: (Boolean) -> Unit): Connection

internal expect val mqttDispatcher: CoroutineDispatcher