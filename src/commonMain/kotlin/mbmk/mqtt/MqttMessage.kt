package mbmk.mqtt

import mbmk.mqtt.MqttQos.*


data class MqttMessage(
    val topic: String,
    val message: List<Byte>,
    /**
     * Can be [AT_MOST_ONCE], [AT_LEAST_ONCE] or [EXACTLY_ONCE]
     */
    val qos: MqttQos = AT_MOST_ONCE,
    val retain: Boolean = false
) {

    @ExperimentalStdlibApi
    constructor(topic: String, message: String, qos: MqttQos = AT_MOST_ONCE, retain: Boolean = false)
            : this(topic, message.encodeToByteArray().toList(), qos, retain)
}