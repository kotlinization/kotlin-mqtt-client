package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientPublishingTest {

    @SpyK
    private var logger: Logger = TestLogger()

    private lateinit var connectionConfig: MqttConnectionConfig

    private lateinit var client: MqttClient

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        connectionConfig = MqttConnectionConfig(
            serverUri = "tcp://localhost:1883",
            connectionTimeout = 5,
            keepAlive = 5
        )
    }

    @Test
    fun publishQoS0() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        client.publishMessage(MqttMessage("test", "Hello"))
        client.disconnect()
    }

    @Test
    fun publishMultipleQoS0() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        repeat(10) {
            delay(200)
            client.publishMessage(MqttMessage("test", it.toString()))
        }
        client.disconnect()
    }

    @Test
    fun publishQoS1() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        client.publishMessage(MqttMessage("test", "Hello", qos = MqttQos.AT_LEAST_ONCE))
        delay(1000)
        client.disconnect()
    }

    @Test
    fun publishMultipleQoS1() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        val jobs = mutableListOf<Job>()
        repeat(1_000) {
            jobs += launch {
                client.publishMessage(
                    MqttMessage(
                        "test",
                        "Hello",
                        qos = MqttQos.AT_LEAST_ONCE
                    )
                )
            }
        }
        jobs.joinAll()
        client.disconnect()
    }

    @Test
    fun publishQoS2() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        client.publishMessage(
            MqttMessage(
                "test",
                "Hello",
                qos = MqttQos.EXACTLY_ONCE
            )
        )
        assertEquals(MqttConnectionStatus.CONNECTED, client.connectionStatus)
        delay(5_000)
        client.disconnect()
    }
}