package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import kotlinx.coroutines.CoroutineDispatcher

internal expect fun createConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?
): MqttConnection

internal expect val mqttDispatcher: CoroutineDispatcher