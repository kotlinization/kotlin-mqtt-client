package mbmk.mqtt

import mbmk.mqtt.MqttQos.EXACTLY_ONCE


@ExperimentalStdlibApi
suspend fun main() {
    val client = MqttClient(
        connectionConfig = MqttConnectionConfig(
            "tcp://localhost:1883", clientId = "test",
            username = "usertest", password = "test",
            willMessage = MqttMessage("testtopic", "testmessage")
        ),
        logger = TestLogger(),
        onConnectionStatusChanged = {
            println("Connection changed: $it")
        }
    )
    val connected = client.connect()
    println("Connected: $connected")
    client.publish(MqttMessage("TEST", "QOS2", EXACTLY_ONCE))
}
