package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttConnectionStatus
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal class PingRequestTracker(
    localScope: CoroutineScope,
    connectionConfig: MqttConnectionConfig,
    connectionFlow: Flow<MqttConnection?>,
    connectionStatusFlow: Flow<MqttConnectionStatus>,
    errorOccurred: suspend () -> Unit,
    publishPingReq: suspend () -> Unit
) {

    init {
        localScope.launch(Dispatchers.MqttDispatcher) {
            connectionFlow
                .filterNotNull()
                .combine(connectionStatusFlow) { connection, status ->
                    connection to status
                }.collectLatest { (connection, status) ->
                    if (status != MqttConnectionStatus.ESTABLISHING && status != MqttConnectionStatus.CONNECTED) return@collectLatest
                    connection.packetTransitFlow
                        .collectLatest {
                            delay(connectionConfig.halfKeepAliveMilliseconds)
                            publishPingReq()
                            delay(connectionConfig.halfKeepAliveMilliseconds)
                            errorOccurred()
                        }
                }
        }
    }

}
