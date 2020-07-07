package com.github.kotlinizer.mqtt

import com.github.kotlinizer.mqtt.internal.util.toByteList
import com.github.kotlinizer.mqtt.internal.util.toDecodedInt
import com.github.kotlinizer.mqtt.internal.util.toEncodedBytes
import com.github.kotlinizer.mqtt.internal.util.toShort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals


class ExtensionKtTest {

    @Test
    fun smallerThan128() {
        repeat(127) { number ->
            assertEquals(1, number.toEncodedBytes().size)
            assertEquals(number.toByte(), number.toEncodedBytes()[0])
        }
    }

    @Test
    fun between128and16_383() {
        var number = 128
        assertEquals(2, number.toEncodedBytes().size)
        assertEquals(0x80.toByte(), number.toEncodedBytes()[0])
        assertEquals(0x01.toByte(), number.toEncodedBytes()[1])

        number = 16383
        assertEquals(2, number.toEncodedBytes().size)
        assertEquals(0xFF.toByte(), number.toEncodedBytes()[0])
        assertEquals(0x7F.toByte(), number.toEncodedBytes()[1])
    }

    @Test
    fun between16_384and2_097_151() {
        var number = 16_384
        assertEquals(3, number.toEncodedBytes().size)
        assertEquals(0x80.toByte(), number.toEncodedBytes()[0])
        assertEquals(0x80.toByte(), number.toEncodedBytes()[1])
        assertEquals(0x01.toByte(), number.toEncodedBytes()[2])

        number = 2_097_151
        assertEquals(3, number.toEncodedBytes().size)
        assertEquals(0xFF.toByte(), number.toEncodedBytes()[0])
        assertEquals(0xFF.toByte(), number.toEncodedBytes()[1])
        assertEquals(0x7F.toByte(), number.toEncodedBytes()[2])
    }

    @Test
    @ExperimentalCoroutinesApi
    fun encodeDecode() {
        blockThread {
            repeat(100_000) { number ->
                val bytes = number.toEncodedBytes()
                val stream = flowOf(*bytes.toTypedArray()).broadcastIn(this).openSubscription()
                val decoded = stream.toDecodedInt()
                assertEquals(number, decoded)
            }
        }
    }

    @Test
    fun shortEncryptDecrypt() {
        repeat(Short.MAX_VALUE.toInt()) {
            val bytes = it.toShort().toByteList()
            val value = bytes.toShort()
            assertEquals(it.toShort(), value)
        }
    }
}