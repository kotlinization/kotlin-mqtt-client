package kotlinx.milan.mqtt

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class MqttClient(
        connectionConfig: MqttConnectionConfig,
        private val onError: (Throwable) -> Unit = {}
) {

    val connected: Boolean
        get() = connection.connected

    private val connection = connectionConfig.createConnection()

    fun connectAsync(): Deferred<Boolean> {
        return executeAsync {
            return@executeAsync connection.runCatching {
                connect()
            }.onFailure {
                logError("Unable to connect", it)
            }.isSuccess
        }
    }

    private fun logError(message: String, error: Throwable) {
        onError(Throwable(message, error))
    }

    private fun <R> executeAsync(action: suspend () -> R): Deferred<R> {
        return GlobalScope.async { action() }
    }
}