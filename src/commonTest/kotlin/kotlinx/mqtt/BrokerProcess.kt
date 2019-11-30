package kotlinx.mqtt

/**
 * Mosquitto must be installed on device.
 */
fun withBroker(username: Boolean = false, port: Int = 1883, block: suspend () -> Unit) {
    val file = TmpFile()
    var passwordFile: TmpFile? = null
    val process = Process(listOf("mosquitto", "-v", "-c", file.path))
    val userPwConfig: String
    if (username) {
        passwordFile = TmpFile()
        userPwConfig = "allow_anonymous false\n password_file ${passwordFile.path}"
    } else {
        userPwConfig = ""
    }
    try {
        passwordFile?.write(userPassword)
        file.write(
            "port $port\n $userPwConfig"
        )
        process.start()
        blockThread {
            block()
        }
    } catch (t: Throwable) {
        println(t.message)
    } finally {
        process.stop()
        file.delete()
        passwordFile?.delete()
    }
}

/**
 * Username: "user" password: "test"
 */
private val userPassword = "user:$6\$lNTBFqv33EiQiuVg\$oIMD9hyNhpcP65nnMB7CVDdeVZClZs7zJ" +
        "aoEM9VHQhEQCnhE54OTMXHQ/nVCnLUh6u4IdPty7ah6kwCoYsWfng=="
