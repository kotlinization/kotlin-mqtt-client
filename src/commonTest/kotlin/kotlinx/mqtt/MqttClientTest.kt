package kotlinx.mqtt

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.*

class MqttClientTest {

    @SpyK
    var onConnection: (Boolean) -> Unit = { println("Connection changed: $it") }

    @SpyK
    var logger: Logger = TestLogger()

    private lateinit var connectionConfig: MqttConnectionConfig

    private lateinit var client: MqttClient

    private val connectTimeout = 10_000L

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        connectionConfig = MqttConnectionConfig(serverUri = "tcp://localhost:1883", connectionTimeout = 5)
    }

    // Connecting

    @Test
    @ExperimentalCoroutinesApi
    fun connectToBroker() = withBroker {
        client = MqttClient(connectionConfig, logger, onConnection)
        val connect = client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
        assertTrue { client.connected }
        assertTrue { connect.async.awaitSync() }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun multipleConnectionsWithSameClient() = withBroker {
        client = MqttClient(connectionConfig, logger, onConnection)
        repeat(1_000) {
            assertTrue { client.connect().async.awaitSync() }
        }
        verify(exactly = 1, timeout = connectTimeout) { onConnection(true) }
    }

    @Test
    fun connectToBrokerAnotherPort() = withBroker(port = 12345) {
        connectionConfig = connectionConfig.copy(serverUri = "tcp://localhost:12345")
        client = MqttClient(connectionConfig, logger, onConnection)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
    }

    @Test
    fun connectToBrokerWithUserPass() = withBroker(username = true) {
        connectionConfig = connectionConfig.copy(username = "user", password = "test")
        client = MqttClient(connectionConfig, logger, onConnection)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
    }

    // Disconnecting

    @Test
    @ExperimentalCoroutinesApi
    fun disconnectFromBroker() = withBroker {
        client = MqttClient(connectionConfig, logger, onConnection)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
        val disconnect = client.disconnect()
        verify(timeout = connectTimeout) { onConnection(false) }
        assertFalse { client.connected }
        assertTrue { disconnect.async.awaitSync() }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun multipleDisconnectsFromBroker() = withBroker {
        client = MqttClient(connectionConfig, logger, onConnection)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
        repeat(1_000) {
            assertTrue { client.disconnect().async.awaitSync() }
        }
        verify(exactly = 1, timeout = connectTimeout) { onConnection(false) }
    }

    @AfterTest
    @ExperimentalCoroutinesApi
    fun clearUp() {
        client.disconnect().async.awaitSync()
    }
}
