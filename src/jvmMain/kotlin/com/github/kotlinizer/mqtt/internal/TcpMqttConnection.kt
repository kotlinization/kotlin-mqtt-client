package com.github.kotlinizer.mqtt.internal

import com.github.kotlinizer.mppktx.coroutines.throwIfCanceled
import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.internal.connection.MqttConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

internal class TcpMqttConnection(
    connectionConfig: MqttConnectionConfig,
    logger: Logger?
) : MqttConnection(connectionConfig, logger) {

    override val receiveChannel = Channel<Byte>(Channel.RENDEZVOUS)

    override val sendChannel = Channel<Byte>(Channel.RENDEZVOUS)

    private var socket = Socket()

    private var inputStream: InputStream = ByteArrayInputStream(ByteArray(0))

    private var outputStream: OutputStream = ByteArrayOutputStream(0)

    private var localScope: CoroutineScope = CoroutineScope(Job() + IO)

    override suspend fun establishConnection(serverUri: String, timeout: Long) {
        clearConnection()
        localScope = CoroutineScope(Job() + IO)
        val uri = URI(serverUri)
        socket = Socket()
        withContext(IO) {
            socket.connect(InetSocketAddress(uri.host, uri.port), timeout.toInt())
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        }
        localScope.launch(IO) {
            while (isActive) {
                try {
                    val byte = inputStream.read().toByte()
                    if (byte != (-1).toByte()) {
                        receiveChannel.send(byte)
                    }
                } catch (t: Throwable) {
                    t.throwIfCanceled()
                    logger?.e(t) {
                        "Error occurred while reading from stream."
                    }
                    connectionBroken()
                }
            }
        }
        localScope.launch(IO) {
            while (isActive) {
                sendChannel.receiveAsFlow().collect {
                    withContext(IO) {
                        try {
                            outputStream.write(it.toInt())
                        } catch (t: Throwable) {
                            t.throwIfCanceled()
                            logger?.e(t) {
                                "Error occurred while writing to stream."
                            }
                            connectionBroken()
                        }
                    }
                }
            }
        }
    }

    override fun clearConnection() {
        runCatching {
            socket.use {
                outputStream.flush()
                socket.shutdownInput()
                socket.shutdownOutput()
            }
        }
        localScope.cancel()
    }
}