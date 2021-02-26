package com.github.kotlinizer.mqtt

class TestLogger(
    private val prefix: String = ""
) : Logger(Level.TRACE) {

    override fun logError(message: String, throwable: Throwable?) {
        println("$milliseconds [E] $prefix $message Throwable: $throwable")
    }

    override fun logDebug(message: String) {
        println("$milliseconds [D] $prefix $message")
    }

    override fun logTrace(message: String) {
        println("$milliseconds [T] $prefix $message")
    }
}