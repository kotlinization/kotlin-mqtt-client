package kotlinx.mqtt

object MqttQos {

    const val AT_MOST_ONCE: Byte = 0

    const val AT_LEAST_ONCE: Byte = 1

    const val EXACTLY_ONCE: Byte = 2
}