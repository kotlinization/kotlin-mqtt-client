package kotlinx.mqtt

expect class Process(commands: List<String>) {

    fun start()

    fun stop()
}

expect class TmpFile() {

    val path: String

    fun write(data: String)

    fun delete()
}