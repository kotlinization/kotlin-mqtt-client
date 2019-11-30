package kotlinx.mqtt


data class MqttMessage(
    val topic: String,
    val message: List<Byte>,
    val qos: MqttQos = MqttQos.AT_MOST_ONCE,
    val retain: Boolean = false
) {

    @UseExperimental(ExperimentalStdlibApi::class)
    constructor(topic: String, message: String, qos: MqttQos = MqttQos.AT_MOST_ONCE, retain: Boolean = false)
            : this(topic, message.encodeToByteArray().toList(), qos, retain)
}