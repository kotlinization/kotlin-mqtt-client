package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import io.mockk.MockKAnnotations
import io.mockk.Ordering
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientConnectionTest {

    @MockK(relaxed = true)
    private lateinit var onConnection: (MqttConnectionStatus) -> Unit

    @SpyK
    private var logger: Logger = TestLogger()

    private lateinit var connectionConfig: MqttConnectionConfig

    private lateinit var client: MqttClient

    private val connectTimeout = 10_000L

    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        scope = CoroutineScope(Job())
        connectionConfig = MqttConnectionConfig(
            serverUri = "tcp://localhost:1883",
            connectionTimeout = 5,
            keepAlive = 5
        )
    }

    @Test
    @ExperimentalCoroutinesApi
    fun connectToBroker() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTING)
            onConnection(MqttConnectionStatus.ESTABLISHING)
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithTimeout() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect(10_000)
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        client.disconnect()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun connectToBrokerWithoutBroker() = blockThread {
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTING)
            onConnection(MqttConnectionStatus.ERROR)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun multipleConnectionsWithSameClient() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        repeat(1_000) {
            client.connect()
        }
        verify(timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTING)
            onConnection(MqttConnectionStatus.ESTABLISHING)
            onConnection(MqttConnectionStatus.CONNECTED)
        }

        client.disconnect()
    }

    @Test
    fun connectToBrokerAnotherPort() = withBroker(port = 12345) {
        connectionConfig = connectionConfig.copy(serverUri = "tcp://localhost:12345")
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithUserPass() = withBroker(username = true) {
        connectionConfig = connectionConfig.copy(username = "user", password = "test")
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithWrongUserPass() = withBroker(username = true) {
        connectionConfig = connectionConfig.copy(username = "user", password = "wrong")
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTING)
            onConnection(MqttConnectionStatus.ESTABLISHING)
            onConnection(MqttConnectionStatus.ERROR)
        }
    }

    @Test
    @ExperimentalStdlibApi
    fun connectToBrokerWithWill() = withBroker {
        connectionConfig = connectionConfig.copy(willMessage = MqttMessage("test", "will"))
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        client.disconnect()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun disconnectFromBroker() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        client.disconnect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.DISCONNECTED)
        }
        assertEquals(MqttConnectionStatus.DISCONNECTED, client.connectionStatus)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun multipleDisconnectsFromBroker() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        repeat(1_000) {
            client.disconnect()
        }
        verify(exactly = 1, timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.DISCONNECTED)
        }
    }

    @Test
    fun keepConnectionActive() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.addConnectionListener()
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }

        delay(10000)
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)

        client.disconnect()
    }

    @AfterTest
    fun clear() {
        scope.cancel()
    }

    private suspend fun MqttClient.addConnectionListener() {
        var firstReceived = false
        scope.launch {
            connectionStatusStateFlow.collect {
                if (firstReceived) {
                    onConnection(it)
                } else {
                    firstReceived = true
                }
            }
        }
        while (!firstReceived) {
            delay(10)
        }
    }

}
