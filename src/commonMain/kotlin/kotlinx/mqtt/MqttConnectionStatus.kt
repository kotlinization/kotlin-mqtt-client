package kotlinx.mqtt

enum class MqttConnectionStatus {
    /**
     * Client is disconnected.
     */
    DISCONNECTED,
    /**
     * Client is creating connection with server.
     */
    CONNECTING,
    /**
     * Client is connected, but is still establishing connection.
     */
    ESTABLISHING,
    /**
     * Client is connected and connection is established.
     * Client can start sending and receiving messages.
     */
    CONNECTED,
    /**
     * Client is disconnecting from server.
     */
    DISCONNECTING,
    /**
     * Client is in error state, mostly when there is some socket error.
     */
    ERROR
}