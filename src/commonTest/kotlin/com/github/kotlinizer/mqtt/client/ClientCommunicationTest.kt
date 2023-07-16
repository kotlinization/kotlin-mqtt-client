package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.MqttQos
import com.github.kotlinizer.mqtt.TestLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClientCommunicationTest {

    private companion object{
        private const val MQTT_TEST_TOPIC = "mqtt_test"
    }

    private val connectionConfig = MqttConnectionConfig(
        serverUri = "tcp://test.mosquitto.org:1883",
        connectionTimeout = 5,
        keepAlive = 5
    )

    @Test
    fun publishAndReceiveQoS0Message() = runBlocking {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage(MQTT_TEST_TOPIC, "Test message.", MqttQos.AT_MOST_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe(MQTT_TEST_TOPIC)
        client1.publishMessage(message)

        assertEquals(message, subscribeFlow.first())
    }

    @Test
    fun publishAndReceiveQoS1Message() = runBlocking {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage(MQTT_TEST_TOPIC, "Test message.", MqttQos.AT_LEAST_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe(MQTT_TEST_TOPIC)
        client1.publishMessage(message)

        assertEquals(message.copy(qos = MqttQos.AT_MOST_ONCE), subscribeFlow.first())
    }

    @Test
    fun publishAndReceiveQoS2Message() = runBlocking {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage(MQTT_TEST_TOPIC, "Test message.", MqttQos.EXACTLY_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe(MQTT_TEST_TOPIC)
        client1.publishMessage(message)

        assertEquals(message.copy(qos = MqttQos.AT_MOST_ONCE), subscribeFlow.first())
    }

    @Test
    fun `subscribe and receive only on topic that matches sent message`() = runBlocking {
        val client1 = MqttClient(TestLogger("C1"))
        val client2 = MqttClient(TestLogger("C2"))
        val message = MqttMessage(MQTT_TEST_TOPIC, "Test message.", MqttQos.AT_MOST_ONCE)

        client1.connect(connectionConfig)
        client2.connect(connectionConfig)

        val subscribeFlow = client2.subscribe(MQTT_TEST_TOPIC)
        val emptyFlow = client2.subscribe("no_message")
        client1.publishMessage(message)

        assertEquals(message, subscribeFlow.first())
        assertNull(withTimeoutOrNull(200) { emptyFlow.first() })
    }
}