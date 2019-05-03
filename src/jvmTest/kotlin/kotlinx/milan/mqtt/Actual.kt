package kotlinx.milan.mqtt


actual class Process actual constructor(private val commands: List<String>) {

    private lateinit var process: java.lang.Process

    actual fun start() {
        process = ProcessBuilder(commands).inheritIO().start()
    }

    actual fun stop() {
        println("Destroying process.")
        process.destroyForcibly()
    }
}