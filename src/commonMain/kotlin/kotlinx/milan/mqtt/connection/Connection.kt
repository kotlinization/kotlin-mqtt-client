package kotlinx.milan.mqtt.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class Connection {

    abstract val connected: Boolean

    private val connectionMutex = Mutex()

    /**
     * @throws Throwable
     */
    suspend fun connect(): Boolean {
        connectionMutex.withLock {
            if (connected) {
                return true
            }
            return establishConnection()
        }
    }

    /**
     * @throws Throwable
     */
    abstract fun establishConnection(): Boolean

}