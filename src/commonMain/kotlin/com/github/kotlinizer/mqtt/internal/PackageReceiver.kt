package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mppktx.coroutines.throwIfCanceled
import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal class PackageReceiver(
    localScope: CoroutineScope,
    connectionFlow: Flow<MqttConnection?>,
    logger: Logger?,
    errorOccurred: suspend () -> Unit,
    packetReceived: (MqttReceivedPacket) -> Unit
) {

    init {
        localScope.launch(mqttDispatcher) {
            connectionFlow
                .filterNotNull()
                .collectLatest { mqttConnection ->
                    try {
                        mqttConnection.packetFlow
                            .collect { packet ->
                                packetReceived(packet)
                            }
                    } catch (t: Throwable) {
                        t.throwIfCanceled()
                        logger?.e(t) {
                            "Error while receiving packet."
                        }
                        errorOccurred()
                    }
                }
        }
    }
}