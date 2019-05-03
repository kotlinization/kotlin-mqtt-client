package kotlinx.mqtt

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class MqttClientTest {

    @SpyK
    var onConnection: (Boolean) -> Unit = { println("Connection changed: $it") }

    @SpyK
    var onError: (Throwable) -> Unit = { println("${it.message}. Cause: ${it.cause?.message}") }

    lateinit var connectionConfig: MqttConnectionConfig

    private val connectTimeout = 10_000L

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        connectionConfig = MqttConnectionConfig(serverUri = "tcp://localhost:1883", connectionTimeout = 5)
    }

    @Test
    fun connectToBroker() = withBroker {
        val client = MqttClient(connectionConfig, onConnection, onError)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
    }

    @Test
    fun connectToBrokerAnotherPort() = withBroker(port = 12345) {
        connectionConfig = connectionConfig.copy(serverUri = "tcp://localhost:12345")
        val client = MqttClient(connectionConfig, onConnection, onError)
        client.connect()
        verify(timeout = connectTimeout) { onConnection(true) }
    }
}

