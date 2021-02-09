package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlin.test.BeforeTest
import kotlin.test.Test


class ClientSubscribeTest {

    @SpyK
    private var logger: Logger = TestLogger()

    @SpyK
    private var listener: MqttMessageListener = {
        println("New message: $it")
    }

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
    @ExperimentalStdlibApi
    fun subscribeQoS0() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        client.subscribe("test", listener)
        delay(1_000)
        val message = MqttMessage("test", "Hello")
        client.publishMessage(message)
        verify(timeout = 1_000) {
            listener(message)
        }
        client.disconnect()
    }

    @Test
    @ExperimentalStdlibApi
    fun subscribeQoS0Retain() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        val message = MqttMessage("test", "Hello", retain = true)
        client.publishMessage(message)
        delay(2_000)

        val client2 = MqttClient(connectionConfig, logger)
        client2.connect(10_000)
        client2.subscribe("test", listener)

        verify(timeout = 10_000) {
            listener(message)
        }

        client.disconnect()
        client2.disconnect()
    }
}