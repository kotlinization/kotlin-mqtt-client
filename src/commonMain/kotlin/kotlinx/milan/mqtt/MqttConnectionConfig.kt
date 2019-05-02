package kotlinx.milan.mqtt

data class MqttConnectionConfig(
    val serverUri: String,
    val clientId: String = "",
    val username: String? = null,
    val password: String? = null,
    val cleanSession: Boolean = true,
    val keepAlive: Short = 30,
    val willMessage: MqttMessage? = null
)