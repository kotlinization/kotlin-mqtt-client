package kotlinx.milan.mqtt.connection

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.milan.mqtt.MqttConnectionConfig

internal abstract class Connection {

    abstract val connected: Boolean

    private val connectionMutex = Mutex()

    /**
     * @throws Throwable
     */
    suspend fun connect(connectionConfig: MqttConnectionConfig): Boolean {
        connectionMutex.withLock {
            if (connected) {
                return true
            }
            establishConnection(connectionConfig.serverUri)
            delay(2000)
            writeMessage(Connect(connectionConfig))
            return true
        }
    }

    /**
     * @throws Throwable
     */
    abstract fun establishConnection(serverUri: String)

    /**
     * @throws Throwable
     */
    protected abstract fun writeMessage(mqttPacket: MqttPacket)

}