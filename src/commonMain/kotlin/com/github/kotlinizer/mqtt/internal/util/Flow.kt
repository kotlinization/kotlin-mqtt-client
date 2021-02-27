package com.github.kotlinizer.mqtt.internal.util

import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import com.github.kotlinizer.mqtt.internal.connection.packet.received.MqttReceivedPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal fun Flow<MqttConnection?>.createPacketFlow(
    localScope: CoroutineScope
): Flow<MqttReceivedPacket> {
    return filterNotNull()
        .flatMapLatest { it.packetFlow }
        .shareIn(localScope, SharingStarted.Lazily)
}