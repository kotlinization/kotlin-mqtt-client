package kotlinx.milan.mqtt

data class MqttConnectionConfig(
    val serverUri: String,
    val cleanSession: Boolean = true,
    val keepAlive: Short = 30,
    val clientId: String = ""
)