package mbmk.mqtt.database

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mbmk.mqtt.Logger
import mbmk.mqtt.MqttPacket
import mbmk.mqtt.MqttQos
import mbmk.mqtt.internal.connection.packet.Publish
import mbmk.mqtt.internal.connection.packet.sent.PubRel
import mbmk.mqtt.internal.connection.packet.sent.Subscribe

@Suppress("UNCHECKED_CAST")
abstract class MessageDatabase(
    protected val logger: Logger?
) {

    private val messageMutex = Mutex()

    internal suspend fun <T : MqttPacket> savePacket(mqttPacket: T): T {
        return when (mqttPacket) {
            is Publish -> savePublish(mqttPacket)
            is Subscribe -> savingPacket(mqttPacket)
            is PubRel -> savingPubRel(mqttPacket)
            else -> mqttPacket
        } as T
    }

    internal suspend fun deletePacket(packetIdentifier: Short) {
        messageMutex.withLock {
            removeMessage(packetIdentifier)
        }
    }

    protected abstract fun generatePackageId(): Short

    protected open fun storeMessage(mqttPacket: MqttPacket) {
        logger?.t { "Storing message: $mqttPacket." }
    }

    protected open fun removeMessage(packetIdentifier: Short) {
        logger?.t { "Deleting message with packetIdentifier: $packetIdentifier" }
    }

    private suspend fun savePublish(mqttPacket: Publish): Publish {
        return when (mqttPacket.mqttMessage.qos) {
            MqttQos.AT_MOST_ONCE -> mqttPacket
            MqttQos.AT_LEAST_ONCE, MqttQos.EXACTLY_ONCE -> savingPacket(mqttPacket)
        }
    }

    private suspend fun <T : MqttPacket> savingPacket(mqttPacket: T): T {
        messageMutex.withLock {
            val packet = mqttPacket.updateIdentifiablePacket(generatePackageId())
            storeMessage(packet)
            return packet
        }
    }

    private suspend fun savingPubRel(pubRel: PubRel): PubRel {
        messageMutex.withLock {
            removeMessage(pubRel.packetIdentifier)
            storeMessage(pubRel)
        }
        return pubRel
    }

    private fun <T : MqttPacket> T.updateIdentifiablePacket(mqttPacket: Short): T {
        return when (this) {
            is Publish -> copy(packetIdentifier = mqttPacket)
            else -> {
                logger?.e { "Unsupported class: $this" }
                this
            }
        } as T
    }
}