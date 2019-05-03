package kotlinx.milan.mqtt

class BrokerProcess {

    private val process: Process = Process(listOf("mosquitto", "-v"))

    fun withBroker(block: () -> Unit) {
        try {
            process.start()
            block()
        } finally {
            process.stop()
        }
    }
}