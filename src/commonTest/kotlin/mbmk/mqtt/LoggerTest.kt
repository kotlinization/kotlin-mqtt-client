package mbmk.mqtt

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.verifySequence
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LoggerTest {


    @MockK(relaxed = true)
    lateinit var logError: (String, Throwable?) -> Unit

    @MockK(relaxed = true)
    lateinit var logDebug: (String) -> Unit

    @MockK(relaxed = true)
    lateinit var logTrace: (String) -> Unit

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun loggerLevelFatal() {
        val logger: Logger = createLogger(Logger.Level.FATAL)

        logger.e { "Error" }
        logger.d { "Debug" }
        logger.t { "Trace" }

//        verifySequence {
//        }
    }

    @Test
    fun loggerLevelError() {
        val logger: Logger = createLogger((Logger.Level.ERROR))

        logger.e { "Error" }
        logger.d { "Debug" }
        logger.t { "Trace" }

        verifySequence {
            logError("Error", null)
        }
    }

    @Test
    fun loggerLevelWarning() {
        val logger: Logger = createLogger((Logger.Level.WARNING))

        logger.e { "Error" }
        logger.d { "Debug" }
        logger.t { "Trace" }

        verifySequence {
            logError("Error", null)
        }
    }

    @Test
    fun loggerLevelDebug() {
        val logger: Logger = createLogger((Logger.Level.DEBUG))

        logger.e { "Error" }
        logger.d { "Debug" }
        logger.t { "Trace" }

        verifySequence {
            logError("Error", null)
            logDebug("Debug")
        }
    }

    @Test
    fun loggerLevelTrace() {
        val logger: Logger = createLogger((Logger.Level.TRACE))

        logger.e { "Error" }
        logger.d { "Debug" }
        logger.t { "Trace" }

        verifySequence {
            logError("Error", null)
            logDebug("Debug")
            logTrace("Trace")
        }
    }

    @AfterTest
    fun clearAndCheck() {
        confirmVerified(logError)
        confirmVerified(logDebug)
        confirmVerified(logTrace)
    }

    private fun createLogger(level: Logger.Level): Logger {
        return TestingLogger(level, logError, logDebug, logTrace)
    }


    private class TestingLogger(
        level: Logger.Level,
        private val error: (String, Throwable?) -> Unit,
        private val debug: (String) -> Unit,
        private val trace: (String) -> Unit
    ) : Logger(level) {

        override fun logError(message: String, throwable: Throwable?) {
            error(message, throwable)
        }

        override fun logDebug(message: String) {
            debug(message)
        }

        override fun logTrace(message: String) {
            trace(message)
        }
    }
}