package kotlinx.milan.mqtt

expect class Process(commands: List<String>) {

    fun start()

    fun stop()
}