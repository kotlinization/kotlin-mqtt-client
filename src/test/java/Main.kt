import kotlinx.coroutines.delay
import org.milan.mqtt.MqttClient
import org.milan.mqtt.MqttConnectionConfig
import java.net.URI

suspend fun main() {
    val client = MqttClient(MqttConnectionConfig(URI("tcp://localhost:1883"))) {
        it.printStackTrace()
    }
    val connected = client.connectAsync()
    println("Connected: $connected")
    println("Broker is connected: ${client.connected}")
    delay(10000)
    println("Broker is connected: ${client.connected}")
    connected.await()
    delay(1000)
    println("Broker is connected: ${client.connected}")
}