package kotlinx.milan.mqtt.packet


internal interface MqttPacket

internal val types = mapOf(
    1.toByte() to Connect::class,
    2.toByte() to Connack::class,
    14.toByte() to Disconnect::class
)

internal val reverseTypes = types.map { it.value to it.key }.toMap()
