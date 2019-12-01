package kotlinx.mqtt

class TestLogger() : Logger(Level.TRACE) {

    override fun logError(message: String, throwable: Throwable?) {
        println("[E] $message Throwable: $throwable")
    }

    override fun logDebug(message: String) {
        println("[D] $message")
    }

    override fun logTrace(message: String) {
        println("[T] $message")
    }
}