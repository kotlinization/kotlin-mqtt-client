package com.github.kotlinizer.mqtt

abstract class Logger(private val level: Level) {

    fun e(throwable: Throwable? = null, message: () -> String = { "" }) {
        if (level >= Level.ERROR) {
            logError(message(), throwable)
        }
    }

    fun d(message: () -> String) {
        if (level >= Level.DEBUG) {
            logDebug(message())
        }
    }

    fun t(message: () -> String) {
        if (level >= Level.TRACE) {
            logTrace(message())
        }
    }

    protected abstract fun logError(message: String, throwable: Throwable?)

    protected abstract fun logDebug(message: String)

    protected abstract fun logTrace(message: String)

    enum class Level {
        FATAL,
        ERROR,
        WARNING,
        DEBUG,
        TRACE
    }
}