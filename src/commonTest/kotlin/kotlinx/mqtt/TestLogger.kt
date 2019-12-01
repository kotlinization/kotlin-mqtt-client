package kotlinx.mqtt

class TestLogger() : Logger(Level.TRACE) {

    override fun logError(message: String, throwable: Throwable?) {
        println("$millies [E] $message Throwable: $throwable")
    }

    override fun logDebug(message: String) {
        println("$millies [D] $message")
    }

    override fun logTrace(message: String) {
        println("$millies [T] $message")
    }
}