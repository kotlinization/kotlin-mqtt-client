package com.github.kotlinizer.mqtt.jvm.sample

import com.github.kotlinizer.mqtt.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private val timeFormatter by lazy {
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
}

data class Log(
    val level: Logger.Level,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {

    val timeMessage: String by lazy {
        timeFormatter.format(Instant.ofEpochMilli(timestamp))
    }
}

class FlowLogger private constructor(
    private val logs: MutableStateFlow<List<Log>>
) : Logger(Level.TRACE), StateFlow<List<Log>> by logs {

    constructor() : this(MutableStateFlow(listOf()))

    override fun logError(message: String, throwable: Throwable?) {
        throwable?.printStackTrace(System.out)
        val msg = " $message \" +\n${throwable?.stackTraceToString()?.replace("\t", "          ")}\""
        logs.value += Log(Level.ERROR, msg)
    }

    override fun logDebug(message: String) {
        logs.value += Log(
            level = Level.DEBUG,
            message = message
        )
    }

    override fun logTrace(message: String) {
        logs.value += Log(
            level = Level.TRACE,
            message = message
        )
    }

    fun clearLogs() {
        logs.value = emptyList()
    }
}