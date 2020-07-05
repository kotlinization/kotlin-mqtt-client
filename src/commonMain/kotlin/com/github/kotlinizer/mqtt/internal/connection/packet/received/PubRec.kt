package com.github.kotlinizer.mqtt.internal.connection.packet.received

internal data class PubRec(override val packetIdentifier: Short) : MqttReceivedPacket