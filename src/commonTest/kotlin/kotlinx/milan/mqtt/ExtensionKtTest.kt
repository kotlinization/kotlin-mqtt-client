package kotlinx.milan.mqtt

import toEncodedBytes
import kotlin.test.Test
import kotlin.test.assertEquals


internal class ExtensionKtTest {

    @Test
    fun smallerThan128() {
        repeat(127) { number ->
            assertEquals(1, number.toEncodedBytes().size)
            assertEquals(number.toByte(), number.toEncodedBytes()[0])
        }
    }

    @Test
    fun between128and16_383(){
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
    fun between16_384and2_097_151(){
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
}