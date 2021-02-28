package com.github.kotlinizer.mqtt.jvm.sample

import com.github.kotlinizer.mqtt.Logger

class SampleLogger : Logger(Level.FATAL) {

    override fun logError(message: String, throwable: Throwable?) {
        println(
            "${System.currentTimeMillis()} [E] $message " +
                    "${throwable?.message} ${throwable?.stackTraceToString()}"
        )
    }

    override fun logDebug(message: String) {
        println("${System.currentTimeMillis()} [D] $message")
    }

    override fun logTrace(message: String) {
        println("${System.currentTimeMillis()} [T] $message")
    }
}