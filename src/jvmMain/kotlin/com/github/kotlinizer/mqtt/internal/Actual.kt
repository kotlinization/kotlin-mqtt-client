package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import kotlinx.coroutines.Dispatchers

internal actual val mqttDispatcher = Dispatchers.IO

internal actual fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?,
    onConnectionChanged: (Boolean) -> Unit
): MqttConnection {

    return TcpMqttConnection(connectionConfig, logger, onConnectionChanged)
}
