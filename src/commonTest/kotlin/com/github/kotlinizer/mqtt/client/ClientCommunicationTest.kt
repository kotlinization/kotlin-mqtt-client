package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.MqttQos
import com.github.kotlinizer.mqtt.TestLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClientCommunicationTest {

    private val connectionConfig = MqttConnectionConfig(
        serverUri = "tcp://localhost:1883",
        connectionTimeout = 5,
        keepAlive = 5
    )

    @Test
    fun publishAndReceiveQoS0Message() = withBroker {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage("test", "Test message.", MqttQos.AT_MOST_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe("test")
        client1.publishMessage(message)

        assertEquals(message, subscribeFlow.first())
    }

    @Test
    fun publishAndReceiveQoS1Message() = withBroker {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage("test", "Test message.", MqttQos.AT_LEAST_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe("test")
        client1.publishMessage(message)

        assertEquals(message.copy(qos = MqttQos.AT_MOST_ONCE), subscribeFlow.first())
    }

    @Test
    fun publishAndReceiveQoS2Message() = withBroker {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage("test", "Test message.", MqttQos.EXACTLY_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe("test")
        client1.publishMessage(message)

        assertEquals(message.copy(qos = MqttQos.AT_MOST_ONCE), subscribeFlow.first())
    }

    @Test
    fun `subscribe and receive only on topic that matches sent message`() = withBroker {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage("test", "Test message.", MqttQos.AT_MOST_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe("test")
        val emptyFlow = client2.subscribe("no_message")
        client1.publishMessage(message)

        assertEquals(message, subscribeFlow.first())
        assertNull(withTimeoutOrNull(200) { emptyFlow.first() })
    }
}