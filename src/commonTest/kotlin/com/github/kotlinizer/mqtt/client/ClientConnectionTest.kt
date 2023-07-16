package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientConnectionTest {

    private val logger: Logger = TestLogger()

    private val connectionConfig = MqttConnectionConfig(
        serverUri = "tcp://test.mosquitto.org:1883",
        connectionTimeout = 5,
        keepAlive = 5
    )

    private val connectionConfigAuthorized = MqttConnectionConfig(
        serverUri = "tcp://test.mosquitto.org:1884",
        connectionTimeout = 5,
        keepAlive = 5,
        username = "ro",
        password = "readonly",
    )

    private val connectionConfigNoBroker = MqttConnectionConfig(
        serverUri = "tcp://localhost:80"
    )

    @Test
    fun connectToBroker() = runBlocking {
        val client = MqttClient(logger)

        client.connect(connectionConfig)

        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithoutBroker() = runBlocking {
        val client = MqttClient(logger)

        client.connect(connectionConfigNoBroker)

        assertEquals(MqttConnectionStatus.ERROR, client.connectionStatus)
    }

    @Test
    fun connectToBrokerWithUserPass() = runBlocking {
        val client = MqttClient(logger)

        client.connect(connectionConfigAuthorized)

        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }

    @Test
    fun disconnectFromBroker() = runBlocking {
        val client = MqttClient(logger)

        client.connect(connectionConfig)

        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)

        client.disconnect()
        assertEquals(MqttConnectionStatus.DISCONNECTED, client.connectionStatus)
    }

    @Test
    fun keepConnectionActive() = runBlocking {
        val client = MqttClient(logger)

        client.connect(connectionConfig)
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)

        delay(10_000)
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }
}
