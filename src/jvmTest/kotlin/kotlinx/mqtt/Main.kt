package kotlinx.mqtt

import kotlinx.coroutines.delay
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@ExperimentalStdlibApi
suspend fun main() {
    val client = MqttClient(
        connectionConfig = MqttConnectionConfig(
            "tcp://localhost:1883", clientId = "test",
            username = "usertest", password = "test",
            willMessage = MqttMessage("testtopic", "testmessage")
        ),
        logger = TestLogger(),
        onConnectionStatusChanged = {
            println("Connection changed: $it")
        }
    )
    repeat(10) {
        val connected = client.connect()
        println("Connected: $connected")
//        println("Broker is connected: ${client.connected}")
//        assertTrue { client.connected }
//        delay(5000)
//        val disconnect = client.disconnect()
//        println("Disconnected: ${disconnect.await()}")
//        assertFalse { client.connected }
        delay(5000)
    }
}
