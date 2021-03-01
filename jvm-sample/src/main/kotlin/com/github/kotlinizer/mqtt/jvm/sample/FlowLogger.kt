package com.github.kotlinizer.mqtt.jvm.sample

import com.github.kotlinizer.mqtt.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow


class FlowLogger private constructor(
    private val logs: MutableStateFlow<List<String>>
) : Logger(Level.TRACE), Flow<List<String>> by logs {

    constructor() : this(MutableStateFlow(mutableListOf()))

    override fun logError(message: String, throwable: Throwable?) {
        logs.value += "${System.currentTimeMillis()} [E] $message " +
                "${throwable?.message} ${throwable?.stackTraceToString()}"

    }

    override fun logDebug(message: String) {
        logs.value += "${System.currentTimeMillis()} [D] $message"
    }

    override fun logTrace(message: String) {
        logs.value += "${System.currentTimeMillis()} [T] $message"
    }
}