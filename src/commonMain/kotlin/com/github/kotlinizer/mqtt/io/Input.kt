package com.github.kotlinizer.mqtt.io

import kotlinx.coroutines.channels.ReceiveChannel

interface Input {
    suspend fun read(): Byte
}

fun ReceiveChannel<Byte>.toInput(): Input {
    return object : Input {
        override suspend fun read(): Byte {
            return receive()
        }
    }
}

fun List<Byte>.toInput(): Input {
    var index = 0
    return object : Input {
        override suspend fun read(): Byte {
            return get(index++)
        }
    }
}