package kotlinx.mqtt

import kotlinx.mqtt.internal.createConnection


class MqttClient(
    val connectionConfig: MqttConnectionConfig,
    private val logger: Logger,
    onConnectionChanged: (Boolean) -> Unit = {}
) {

    val connected: Boolean
        get() = connection.connected

    private val connection by lazy { createConnection(connectionConfig, logger, onConnectionChanged) }

    fun connect() = MqttResult {
        try {
            connection.connect()
        } catch (t: Throwable) {
            logger.e(t) { "Unable to connect." }
            false
        }
    }

    fun disconnect() = MqttResult {
        try {
            connection.disconnect()
        } catch (t: Throwable) {
            logger.e(t) { "Error while disconnecting." }
            false
        }
    }
}