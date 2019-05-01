package kotlinx.milan.mqtt


class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    private val onError: (Throwable) -> Unit = {}
) {

    val connected: Boolean
        get() = connection.connected

    private val connection = createConnection()

    fun connect(): MqttResult<Boolean> {
        return MqttResult {
            try {
                connection.connect(connectionConfig)
            } catch (t: Throwable) {
                logError("Unable to connect.", t)
                false
            }
        }
    }

    private fun logError(message: String, error: Throwable) {
        onError(Throwable(message, error))
    }
}