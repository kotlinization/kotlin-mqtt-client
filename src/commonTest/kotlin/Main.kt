import kotlinx.coroutines.delay
import kotlinx.milan.mqtt.MqttClient
import kotlinx.milan.mqtt.MqttConnectionConfig

//import kotlinx.coroutines.delay
//import MqttClient
//import org.milan.mqtt.MqttConnectionConfig
//import java.net.URI

suspend fun main() {
    val client = MqttClient(MqttConnectionConfig("tcp://localhost:1883")) {
        println("${it.message}. Cause: ${it.cause?.message}")
    }
    val connected = client.connectAsync()
    println("Connected: $connected")
    println("Connected: $connected")
    println("Broker is connected: ${client.connected}")
    delay(10000)
    println("Broker is connected: ${client.connected}")
    connected.await()
    delay(1000)
    println("Broker is connected: ${client.connected}")
    println("Connected: ${connected.await()}")
}