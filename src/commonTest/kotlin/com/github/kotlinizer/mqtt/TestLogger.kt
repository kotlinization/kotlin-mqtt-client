package com.github.kotlinizer.mqtt

class TestLogger : Logger(Level.TRACE) {

    override fun logError(message: String, throwable: Throwable?) {
        println("$milliseconds [E] $message Throwable: $throwable")
    }

    override fun logDebug(message: String) {
        println("$milliseconds [D] $message")
    }

    override fun logTrace(message: String) {
        println("$milliseconds [T] $message")
    }
}