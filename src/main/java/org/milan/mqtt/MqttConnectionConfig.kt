package org.milan.mqtt

import java.net.URI

data class MqttConnectionConfig(val serverUri: URI) {

    internal fun createConnection(): Connection {
        return TcpConnection(this)
    }

}