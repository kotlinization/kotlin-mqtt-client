package mbmk.mqtt.client

import io.mockk.MockKAnnotations
import io.mockk.Ordering
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import mbmk.mqtt.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientConnectionTest {

    @MockK(relaxed = true)
    private lateinit var onConnection: (MqttConnectionStatus) -> Unit

    @SpyK
    private var logger: Logger = TestLogger()

    private lateinit var connectionConfig: MqttConnectionConfig

    private lateinit var client: MqttClient

    private val connectTimeout = 10_000L

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        connectionConfig = MqttConnectionConfig(
            serverUri = "tcp://localhost:1883",
            connectionTimeout = 5,
            keepAlive = 5
        )
    }

    @Test
    @ExperimentalCoroutinesApi
    fun connectToBroker() = withBroker {
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        val connect = client.connect()
        verify(timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTING)
            onConnection(MqttConnectionStatus.ESTABLISHING)
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        assertTrue(connect)
        client.disconnect()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun connectToBrokerWithoutBroker() = blockThread {
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect()
        verify(timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTING)
            onConnection(MqttConnectionStatus.ERROR)
            onConnection(MqttConnectionStatus.DISCONNECTED)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun multipleConnectionsWithSameClient() = withBroker {
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        repeat(1_000) {
            assertTrue(client.connect())
        }
        verify(exactly = 1, timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }

        client.disconnect()
    }

    @Test
    fun connectToBrokerAnotherPort() = withBroker(port = 12345) {
        connectionConfig = connectionConfig.copy(serverUri = "tcp://localhost:12345")
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithUserPass() = withBroker(username = true) {
        connectionConfig = connectionConfig.copy(username = "user", password = "test")
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        client.disconnect()
    }

    @Test
    fun connectToBrokerWithWrongUserPass() = withBroker(username = true) {
        connectionConfig = connectionConfig.copy(username = "user", password = "wrong")
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect()
        verify(timeout = connectTimeout, ordering = Ordering.SEQUENCE) {
            onConnection(MqttConnectionStatus.CONNECTING)
            onConnection(MqttConnectionStatus.ESTABLISHING)
            onConnection(MqttConnectionStatus.ERROR)
            onConnection(MqttConnectionStatus.DISCONNECTED)
        }
    }

    @Test
    @ExperimentalStdlibApi
    fun connectToBrokerWithWill() = withBroker {
        connectionConfig = connectionConfig.copy(willMessage = MqttMessage("test", "will"))
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }
        client.disconnect()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun disconnectFromBroker() = withBroker {
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
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
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
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
        client = MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect()
        verify(timeout = connectTimeout) {
            onConnection(MqttConnectionStatus.CONNECTED)
        }

        delay(10000)
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)

        client.disconnect()
    }
}