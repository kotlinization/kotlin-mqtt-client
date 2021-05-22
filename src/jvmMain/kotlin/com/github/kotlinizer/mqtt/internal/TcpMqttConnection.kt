package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

internal class TcpMqttConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?
) : MqttConnection(connectionConfig, logger) {

    private val socket: Socket by lazy {
        Socket()
    }

    private lateinit var inputStream: InputStream

    private lateinit var outputStream: OutputStream

    override val brokerToClientFlow: Flow<Byte>
        get() = channelFlow {
            withContext(IO) {
                while (isActive) {
                    val byte = runCatching {
                        inputStream.read().toByte()
                    }.getOrNull() ?: return@withContext
                    if (byte != (-1).toByte()) {
                        send(byte)
                    }
                }
            }
        }

    override suspend fun connect() {
        val uri = URI(connectionConfig.serverUri)
        withContext(IO) {
            socket.connect(
                InetSocketAddress(uri.host, uri.port),
                connectionConfig.connectionTimeoutMilliseconds.toInt()
            )
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        }
        logger?.t {
            "Connection established."
        }
    }

    override suspend fun disconnectAndClear() {
        withContext(IO) {
            socket.use {
                outputStream.flush()
                socket.shutdownInput()
                socket.shutdownOutput()
            }
        }
    }

    override suspend fun writeBytes(bytes: List<Byte>) {
        withContext(IO) {
            outputStream.write(bytes.toByteArray())
            outputStream.flush()
        }
    }
}