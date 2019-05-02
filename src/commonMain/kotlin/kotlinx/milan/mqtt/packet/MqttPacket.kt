package kotlinx.milan.mqtt.packet


internal abstract class MqttPacket

internal val types = mapOf(
    1.toByte() to Connect::class,
    2.toByte() to Connack::class
)

internal val reverseTypes = types.map { it.value to it.key }.toMap()
