package kotlinx.mqtt

fun withBroker(port: Int = 1883, block: () -> Unit) {
    val file = TmpFile()
    val process = Process(listOf("mosquitto", "-v", "-c", file.path))
    try {
        file.write(
            "port $port\n"
        )
        process.start()
        block()
    } finally {
        process.stop()
        file.delete()
    }
}