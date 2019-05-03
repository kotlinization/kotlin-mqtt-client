package kotlinx.mqtt

import kotlinx.serialization.toUtf8Bytes

data class MqttMessage(
    val topic: String,
    val message: List<Byte>,
    val qos: MqttQos = MqttQos.AT_MOST_ONCE,
    val retain: Boolean = false
) {
    constructor(topic: String, message: String, qos: MqttQos = MqttQos.AT_MOST_ONCE, retain: Boolean = false)
            : this(topic, message.toUtf8Bytes().toList(), qos, retain)
}