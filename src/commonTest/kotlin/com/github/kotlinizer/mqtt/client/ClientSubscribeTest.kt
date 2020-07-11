package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.delay
import kotlin.test.BeforeTest
import kotlin.test.Test


class ClientSubscribeTest {

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
    @ExperimentalStdlibApi
    fun subscribeQoS0() = withBroker {
        client = MqttClient(connectionConfig, logger)
        client.connect(10_000)
        client.subscribe("test")
        delay(1_000)
        client.publishMessage(MqttMessage("test", "Hello"))
        delay(1_000)
        client.disconnect()
    }
}