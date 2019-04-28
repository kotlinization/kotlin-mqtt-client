package kotlinx.milan.mqtt

import kotlinx.coroutines.delay
import kotlin.test.assertTrue

suspend fun main() {
    val client = MqttClient(MqttConnectionConfig("tcp://localhost:1883")) {
        println("${it.message}. Cause: ${it.cause?.message}")
    }
    val connected = client.connect()
    println("Connected: $connected")
    println("Broker is connected: ${client.connected}")
    delay(10000)
    println("Broker is connected: ${client.connected}")
    connected.await()
    delay(1000)
    println("Broker is connected: ${client.connected}")
    println("Connected: ${connected.await()}")
    println("Broker is connected: ${client.connected}")
    assertTrue { client.connected }
}