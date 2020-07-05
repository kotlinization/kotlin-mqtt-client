package com.github.kotlinizer.mqtt.internal.connection.packet.received

internal data class PubComp(override val packetIdentifier: Short) : MqttReceivedPacket