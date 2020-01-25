package mbmk.mqtt

expect class Process(commands: List<String>) {

    fun start()

    fun stop()
}

expect class TmpFile() {

    val path: String

    fun write(data: String)

    fun delete()
}

expect fun blockThread(method: suspend () -> Unit)

expect val milliseconds: Long