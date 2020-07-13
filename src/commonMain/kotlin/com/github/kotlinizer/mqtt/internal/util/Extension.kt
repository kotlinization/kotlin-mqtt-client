package com.github.kotlinizer.mqtt.internal.util

import com.github.kotlinizer.mqtt.MQTTException
import com.github.kotlinizer.mqtt.internal.connection.packet.Publish
import com.github.kotlinizer.mqtt.internal.connection.packet.received.*
import com.github.kotlinizer.mqtt.types
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.experimental.and
import kotlin.experimental.or

internal fun MutableList<Byte>.addShort(short: Short) {
    addAll(short.toByteList())
}

internal fun MutableList<Byte>.addByteList(list: List<Byte>) {
    addShort(list.size.toShort())
    addAll(list)
}

@OptIn(ExperimentalStdlibApi::class)
internal fun MutableList<Byte>.addStringWithLength(string: String) {
    val bytes = string.encodeToByteArray().toTypedArray()
    addAll(bytes.size.toShort().toByteList())
    addAll(bytes)
}


internal fun Short.toByteList(): List<Byte> {
    return listOf(
        toInt().shr(8).toByte(),
        toByte()
    )
}

internal fun List<Byte>.toShort(): Short {
    if (size < 2) throw IllegalArgumentException("List must have at least 2 elements.")
    return (get(0).toInt().shl(8).toShort() + get(1).toUByte().toShort()).toShort()
}

internal infix fun Byte.shl(count: Int): Byte {
    return toInt().shl(count).toByte()
}

internal infix fun Byte.shr(count: Int): Byte {
    return toInt().shr(count).toByte()
}

internal fun Int.toEncodedBytes(): List<Byte> {
    val bytes = mutableListOf<Byte>()
    var x = this
    do {
        val encodedByte = (x % 128).toByte()
        x /= 128
        bytes.add(if (x > 0) encodedByte or 128.toByte() else encodedByte)
    } while (x > 0)
    return bytes
}

private const val MASK = 128.toByte()

private const val STOP = 0.toByte()

@OptIn(ExperimentalStdlibApi::class)
internal suspend fun ReceiveChannel<Byte>.getPacket(): MqttReceivedPacket {
    val header = receive()
    val type = header.toInt().and(0x000000FF).shr(4)
    val size = receiveDecodedInt()
    val bytes = if (size < 1) emptyList() else (0 until size).map {
        receive()
    }
    val kClass = types[type] ?: throw MQTTException("Unknown type $type.")
    return when (kClass) {
        ConnAck::class -> ConnAck(bytes)
        PingResp::class -> PingResp()
        PubAck::class -> PubAck(bytes.toShort())
        PubRec::class -> PubRec(bytes.toShort())
        PubComp::class -> PubComp(bytes.toShort())
        SubAck::class -> SubAck(bytes)
        Publish::class -> {
            Publish.receivePacket(header, bytes)
        }
        else -> throw IllegalArgumentException("Unknown class: $kClass")
    }
}

internal suspend fun ReceiveChannel<Byte>.receiveDecodedInt(): Int {
    var multiplier = 1
    var value = 0
    do {
        val encodedByte = receive()
        value += (encodedByte and 127) * multiplier
        multiplier *= 128
        if (multiplier > 128 * 128 * 128) {
            throw MQTTException("Malformed remaining length.")
        }
    } while ((encodedByte and MASK) != STOP)
    return value
}

@OptIn(ExperimentalStdlibApi::class)
internal suspend fun ReceiveChannel<Byte>.receiveString(): String {
    val lengthBytes = listOf(receive(), receive())
    val length = lengthBytes.toShort()
    return ByteArray(length.toInt()) {
        receive()
    }.decodeToString()
}

internal fun List<Byte>.toReceiveChannel(): ReceiveChannel<Byte> {
    return Channel<Byte>(size).apply {
        forEach {
            offer(it)
        }
        close()
    }
}