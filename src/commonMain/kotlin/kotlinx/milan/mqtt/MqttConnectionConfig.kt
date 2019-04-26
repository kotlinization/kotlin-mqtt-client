package kotlinx.milan.mqtt

data class MqttConnectionConfig(val serverUri: String) {

    internal fun createConnection(): Connection {
        return TestConnection()
    }

}