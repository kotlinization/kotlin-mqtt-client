package kotlinx.milan.mqtt

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

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        connectionConfig = MqttConnectionConfig("tcp://localhost:1883")
    }

    @Test
    fun connectToBroker() = BrokerProcess().withBroker {
        val client = MqttClient(connectionConfig, onConnection, onError)
        client.connect()
        verify(timeout = 10_000) { onConnection(true) }
        verify(timeout = 60_000, exactly = 0) { onError(any()) }
    }

}

