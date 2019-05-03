package kotlinx.mqtt


class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    onConnectionChanged: (Boolean) -> Unit = {},
    private val onError: (Throwable) -> Unit = {}
) {

    val connected: Boolean
        get() = connection.connected

    private val connection = createConnection(onConnectionChanged)

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

    fun disconnect(): MqttResult<Boolean> {
        return MqttResult {
            try {
                connection.disconnect()
            } catch (t: Throwable) {
                logError("Error while disconnecting.", t)
                false
            }
        }
    }

    private fun logError(message: String, error: Throwable) {
        onError(Throwable(message, error))
    }
}