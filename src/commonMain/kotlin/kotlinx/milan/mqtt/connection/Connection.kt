package kotlinx.milan.mqtt.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.milan.mqtt.MqttConnectionConfig
import kotlinx.milan.mqtt.packet.*
import kotlin.properties.Delegates.observable

internal abstract class Connection(
    private val onConnectionChanged: (Boolean) -> Unit
) {

    var connected by observable(false) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            onConnectionChanged(newValue)
        }
    }
        private set

    protected abstract val inputStream: InputStream

    protected abstract val outputStream: OutputStream

    private val connectionMutex = Mutex()

    private val sendingMutex = Mutex()

    private val receivingMutex = Mutex()

    /**
     * @throws Throwable
     */
    suspend fun connect(connectionConfig: MqttConnectionConfig): Boolean {
        connectionMutex.withLock {
            if (connected) {
                return true
            }
            establishConnection(connectionConfig.serverUri)
            writePacket(Connect(connectionConfig))
            val packet = readPacket() as? Connack ?: throw IOException("Wrong packet received.")
            packet.error?.let { throw it }
            connected = true
            return true
        }
    }

    /**
     * @throws Throwable
     */
    suspend fun disconnect(): Boolean {
        connectionMutex.withLock {
            if (!connected) {
                return true
            }
            writePacket(Disconnect())
            breakConnection()
            connected = false
            return true
        }
    }

    /**
     * @throws Throwable
     */
    protected abstract fun establishConnection(serverUri: String)

    /**
     * @throws Throwable
     */
    protected abstract fun breakConnection()

    /**
     * @throws Throwable
     */
    private suspend fun writePacket(mqttPacket: MqttSendingPacket) {
        sendingMutex.withLock {
            outputStream.write(mqttPacket.pack().toByteArray())
        }
    }

    /**
     * @throws Throwable
     */
    private suspend fun readPacket(): MqttReceivingPacket {
        return receivingMutex.withLock {
            inputStream.getPacket()
        }
    }
}