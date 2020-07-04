package mbmk.mqtt

class MQTTException(message: String) : Exception(message)

internal object StopFlowCollection : Throwable()