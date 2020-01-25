package mbmk.mqtt

import mbmk.mqtt.MqttQos.AT_LEAST_ONCE
import mbmk.mqtt.MqttQos.AT_MOST_ONCE
import mbmk.mqtt.MqttQos.EXACTLY_ONCE


data class MqttMessage(
    val topic: String,
    val message: List<Byte>,
    /**
     * Can be [AT_MOST_ONCE], [AT_LEAST_ONCE] or [EXACTLY_ONCE]
     */
    val qos: Byte = AT_MOST_ONCE,
    val retain: Boolean = false
) {

    internal val constrainedQos by lazy {
        qos.coerceIn(AT_MOST_ONCE, EXACTLY_ONCE).toByte()
    }

    @ExperimentalStdlibApi
    constructor(topic: String, message: String, qos: Byte = AT_MOST_ONCE, retain: Boolean = false)
            : this(topic, message.encodeToByteArray().toList(), qos, retain)
}