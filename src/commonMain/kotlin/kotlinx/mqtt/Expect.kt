package kotlinx.mqtt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.mqtt.connection.Connection

internal expect fun createConnection(onConnectionChanged: (Boolean) -> Unit): Connection

internal expect val mqttDispatcher: CoroutineDispatcher