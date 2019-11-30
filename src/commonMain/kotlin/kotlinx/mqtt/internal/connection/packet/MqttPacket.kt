package kotlinx.mqtt.internal.connection.packet


internal interface MqttPacket

internal val types = mapOf(
    1 to Connect::class,
    2 to ConnAck::class,
    14 to Disconnect::class
)

internal val reverseTypes by lazy {
    types.map { it.value to it.key.toByte() }.toMap()
}
