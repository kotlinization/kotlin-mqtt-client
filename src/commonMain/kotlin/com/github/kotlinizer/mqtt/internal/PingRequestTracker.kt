package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

internal class PingRequestTracker(
    private val localScope: CoroutineScope,
    private val logger: Logger?,
    private val connectionFlow: Flow<MqttConnection?>,
    private val errorOccurred: suspend () -> Unit,
    private val publishPingReq: suspend () -> Unit,
) {

    private var pingingJob: Job? = null

    suspend fun startPinging(connectionConfig: MqttConnectionConfig) {
        pingingJob?.cancelAndJoin()
        pingingJob = localScope.launch(Dispatchers.MqttDispatcher) {
            logger?.t { "Started pinging job." }
            var scheduledPing = schedulePing(connectionConfig)
            connectionFlow
                .filterNotNull()
                .collectLatest { connection ->
                    connection.packetTransitFlow.collect {
                        scheduledPing.cancelAndJoin()
                        scheduledPing = schedulePing(connectionConfig)
                    }
                }
        }
    }

    fun stopPinging() {
        pingingJob?.let {
            it.cancel()
            logger?.t { "Canceled pinging job." }
        }
        pingingJob = null
    }

    private fun CoroutineScope.schedulePing(connectionConfig: MqttConnectionConfig) = launch {
        delay(connectionConfig.halfKeepAliveMilliseconds)
        publishPingReq()
        delay(connectionConfig.halfKeepAliveMilliseconds)
        errorOccurred()
    }
}
