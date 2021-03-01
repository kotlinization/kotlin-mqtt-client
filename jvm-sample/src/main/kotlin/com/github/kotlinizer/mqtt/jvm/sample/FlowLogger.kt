package com.github.kotlinizer.mqtt.jvm.sample

import com.github.kotlinizer.mqtt.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class FlowLogger private constructor(
    private val logs: MutableStateFlow<List<String>>
) : Logger(Level.TRACE), Flow<List<String>> by logs {

    constructor() : this(MutableStateFlow(mutableListOf()))

    private val timeFormatter by lazy {
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    private val currentTime: String
        get() = timeFormatter.format(Instant.now())

    override fun logError(message: String, throwable: Throwable?) {
        logs.value += "$currentTime [E] $message " +
                "${throwable?.stackTraceToString()?.replace("\t", "          ")}"

    }

    override fun logDebug(message: String) {
        logs.value += "$currentTime [D] $message"
    }

    override fun logTrace(message: String) {
        logs.value += "$currentTime [T] $message"
    }
}