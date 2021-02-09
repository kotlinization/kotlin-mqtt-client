package com.github.kotlinizer.mqtt

import com.github.kotlinizer.mqtt.client.MqttClient


@ExperimentalStdlibApi
suspend fun main() {
    val client = MqttClient(
        connectionConfig = MqttConnectionConfig(
            "tcp://localhost:1883", clientId = "test",
            username = "usertest", password = "test",
            willMessage = MqttMessage("testtopic", "testmessage")
        ),
        logger = TestLogger()
    )
    val connected = client.connect()
    println("Connected: $connected")
    client.publishMessage(MqttMessage("TEST", "QOS2", MqttQos.EXACTLY_ONCE))
}
