package com.github.kotlinizer.mqtt

import com.github.kotlinizer.mqtt.MqttQos.*

data class MqttMessage(
    val topic: String,
    val content: String,
    /**
     * Can be [AT_MOST_ONCE], [AT_LEAST_ONCE] or [EXACTLY_ONCE]
     */
    val qos: MqttQos = AT_MOST_ONCE,
    val retain: Boolean = false
) {

    constructor(topic: String, message: List<Byte>, qos: MqttQos = AT_MOST_ONCE, retain: Boolean = false)
            : this(topic, message.toByteArray().decodeToString(), qos, retain)

    internal val messageBytes by lazy {
        content.encodeToByteArray().asList()
    }
}