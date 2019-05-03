package kotlinx.mqtt

import kotlinx.io.IOException
import java.io.File
import java.lang.System.currentTimeMillis
import java.lang.System.getProperty
import java.nio.file.Paths.get


actual class Process actual constructor(private val commands: List<String>) {

    private lateinit var process: java.lang.Process

    actual fun start() {
        process = ProcessBuilder(commands).inheritIO().start()
        Thread.sleep(1000) // Wait for process to start
    }

    actual fun stop() {
        process.destroyForcibly()
    }
}

actual class TmpFile {

    actual val path = get(getProperty("java.io.tmpdir"), currentTimeMillis().toString()).toAbsolutePath().toString()

    private val file = File(path)

    init {
        if (!file.createNewFile()) {
            throw IOException("Unable to create new file.")
        }
    }

    actual fun write(data: String) {
        file.outputStream().use {
            it.write(data.toByteArray())
        }
    }

    actual fun delete() {
        file.delete()
    }
}