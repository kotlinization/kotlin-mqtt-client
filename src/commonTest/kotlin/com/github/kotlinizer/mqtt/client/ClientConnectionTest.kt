package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientConnectionTest {

    private var logger: Logger = TestLogger()

    private val connectionConfig = MqttConnectionConfig(
        serverUri = "tcp://localhost:1883",
        connectionTimeout = 5,
        keepAlive = 5
    )

    @Test
    fun connectToBroker() = withBroker {
        val client = MqttClient(connectionConfig, logger)

        client.connect()

        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithoutBroker() = blockThread {
        val client = MqttClient(connectionConfig, logger)

        client.connect()

        assertEquals(MqttConnectionStatus.ERROR, client.connectionStatus)
    }

    @Test
    fun connectToBrokerAnotherPort() = withBroker(port = 12345) {
        val client = MqttClient(connectionConfig.copy(serverUri = "tcp://localhost:12345"), logger)

        client.connect()

        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithUserPass() = withBroker(username = true) {
        val client = MqttClient(connectionConfig.copy(username = "user", password = "test"), logger)

        client.connect()

        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }

    @Test
    fun disconnectFromBroker() = withBroker {
        val client = MqttClient(connectionConfig, logger)

        client.connect()

        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)

        client.disconnect()
        assertEquals(MqttConnectionStatus.DISCONNECTED, client.connectionStatus)
    }

    @Test
    fun keepConnectionActive() = withBroker {
        val client = MqttClient(connectionConfig, logger)


        client.connect()
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)

        delay(10_000)
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }
}
