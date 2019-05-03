package kotlinx.mqtt

data class MqttConnectionConfig(
    val serverUri: String,
    val clientId: String = "",
    val username: String? = null,
    val password: String? = null,
    val cleanSession: Boolean = true,
    val keepAlive: Short = 30,
    val willMessage: MqttMessage? = null,
    /**
     * How much seconds to wait during connecting, 0 means infinite timeout.
     */
    val connectionTimeout: Int = 30
)