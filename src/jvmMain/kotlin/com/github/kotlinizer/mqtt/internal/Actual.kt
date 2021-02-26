package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import kotlinx.coroutines.Dispatchers

internal actual val Dispatchers.MqttDispatcher
    get() = IO

internal actual fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?
): MqttConnection = TcpMqttConnection(connectionConfig, logger)

