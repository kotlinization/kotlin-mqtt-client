package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.TestLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientCommunicationTest {

    private val connectionConfig = MqttConnectionConfig(
        serverUri = "tcp://localhost:1883",
        connectionTimeout = 5,
        keepAlive = 5
    )

    @Test
    fun publishAndReceiveMessage() = withBroker {
        val client1 = MqttClient(connectionConfig, TestLogger("C1"))
        val client2 = MqttClient(connectionConfig, TestLogger("C2"))

        client1.connect()
        client2.connect()

        launch {
            val message = client2.subscribe("test")
                .first()
            assertEquals("Test message.", message.message)
            println("Verified.")
        }

        client1.publishMessage(MqttMessage("test", "Test message."))
    }
}