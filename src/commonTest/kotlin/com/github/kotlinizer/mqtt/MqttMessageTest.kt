package com.github.kotlinizer.mqtt

import kotlin.test.Test
import kotlin.test.assertEquals


internal class MqttMessageTest {

    @Test
    fun testEquals() {
        val message1 = MqttMessage("test", "mess")
        val message2 = MqttMessage("test", "mess")
        assertEquals(message1, message2)
    }
}