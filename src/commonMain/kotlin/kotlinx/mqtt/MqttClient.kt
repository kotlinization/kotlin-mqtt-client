package kotlinx.mqtt

import kotlinx.mqtt.internal.createConnection


class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    onConnectionChanged: (Boolean) -> Unit = {},
    private val onError: (Exception) -> Unit = {}
) {

    val connected: Boolean
        get() = connection.connected

    private val connection by lazy { createConnection(connectionConfig, onConnectionChanged, onError) }

    fun connect() = MqttResult {
        try {
            connection.connect()
        } catch (t: Throwable) {
            logError("Unable to connect.", t)
            false
        }
    }

    fun disconnect() = MqttResult {
        try {
            connection.disconnect()
        } catch (t: Throwable) {
            logError("Error while disconnecting.", t)
            false
        }
    }

    private fun logError(message: String, error: Throwable) {
        onError(Exception(message, error))
    }
}