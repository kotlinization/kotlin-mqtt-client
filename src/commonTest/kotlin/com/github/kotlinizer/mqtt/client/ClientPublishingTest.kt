package com.github.kotlinizer.mqtt.client

import com.github.kotlinizer.mqtt.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientPublishingTest {


    @MockK(relaxed = true)
    private lateinit var onConnection: (MqttConnectionStatus) -> Unit

    @SpyK
    private var logger: Logger = TestLogger()

    private lateinit var connectionConfig: MqttConnectionConfig

    private lateinit var client: MqttClient

    private val connectTimeout = 10_000L

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
    fun publishQoS0() = withBroker {
        client =
            MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect(10_000)
        client.publishMessage(MqttMessage("test", "Hello"))
        client.disconnect()
    }

    @Test
    @ExperimentalStdlibApi
    fun publishMultipleQoS0() = withBroker {
        client =
            MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect(10_000)
        repeat(10) {
            delay(200)
            client.publishMessage(MqttMessage("test", it.toString()))
        }
        client.disconnect()
    }

    @Test
    @ExperimentalStdlibApi
    fun publishQoS1() = withBroker {
        client =
            MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect(10_000)
        client.publishMessage(MqttMessage("test", "Hello", qos = MqttQos.AT_LEAST_ONCE))
        delay(1000)
        client.disconnect()
    }

    @Test
    @ExperimentalStdlibApi
    fun publishMultipleQoS1() = withBroker {
        client =
            MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
        client.connect(10_000)
        val jobs = mutableListOf<Job>()
        repeat(10_00) {
            jobs += GlobalScope.launch {
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
    @ExperimentalStdlibApi
    fun publishQoS2() = withBroker {
        client =
            MqttClient(connectionConfig, logger, onConnectionStatusChanged = onConnection)
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