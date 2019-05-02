package kotlinx.milan.mqtt.connection

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.milan.mqtt.MqttConnectionConfig
import kotlinx.milan.mqtt.packet.*

internal abstract class Connection {

    abstract val connected: Boolean

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
            delay(2000)
            writePacket(Connect(connectionConfig))
            val packet = readPacket() as? Connack ?: throw IOException("Wrong packet received.")
            packet.error?.let { throw it }
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