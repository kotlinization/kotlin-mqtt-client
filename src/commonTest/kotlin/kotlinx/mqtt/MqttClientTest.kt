package kotlinx.mqtt

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MqttClientTest {

    @SpyK
    var onConnection: (Boolean) -> Unit = { println("Connection changed: $it") }

    @SpyK
    var onError: (Throwable) -> Unit = { println("${it.message}. Cause: ${it.cause?.message}") }

    private lateinit var connectionConfig: MqttConnectionConfig

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
        val client = MqttClient(connectionConfig, onConnection, onError)
        val connect = client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
        assertTrue { client.connected }
        assertTrue { connect.async.awaitSync() }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun multipleConnectionsWithSameClient() = withBroker {
        val client = MqttClient(connectionConfig, onConnection, onError)
        repeat(1_000) {
            assertTrue { client.connect().async.awaitSync() }
        }
        verify(exactly = 1, timeout = connectTimeout) { onConnection(true) }
    }

    @Test
    fun connectToBrokerAnotherPort() = withBroker(port = 12345) {
        connectionConfig = connectionConfig.copy(serverUri = "tcp://localhost:12345")
        val client = MqttClient(connectionConfig, onConnection, onError)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
    }

    @Test
    fun connectToBrokerWithUserPass() = withBroker(username = true) {
        connectionConfig = connectionConfig.copy(username = "user", password = "test")
        val client = MqttClient(connectionConfig, onConnection, onError)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
    }

    // Disconnecting

    @Test
    @ExperimentalCoroutinesApi
    fun disconnectFromBroker() = withBroker {
        val client = MqttClient(connectionConfig, onConnection, onError)
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
        val client = MqttClient(connectionConfig, onConnection, onError)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
        repeat(1_000) {
            assertTrue { client.disconnect().async.awaitSync() }
        }
        verify(exactly = 1, timeout = connectTimeout) { onConnection(false) }
    }

}

@ExperimentalCoroutinesApi
private fun <T> Deferred<T>.awaitSync(): T {
    while (!isCompleted) {
    }
    return getCompleted()
}
