package org.jetbrains.ktor.benchmarks.scenarios

import org.junit.*
import java.io.*
import java.net.*

abstract class AbstractBenchmark {
    protected val port = findFreePort()

    protected abstract fun start()

    protected abstract fun stop()

    @Before
    fun setUp() {
        start()
        waitForPort(port)
    }

    @After
    fun teamDown() {
        stop()
    }

    companion object {
        fun findFreePort() = ServerSocket(0).use { it.localPort }
        fun waitForPort(port: Int) {
            do {
                Thread.sleep(50)
                try {
                    Socket("localhost", port).close()
                    break
                } catch (expected: IOException) {
                }
            } while (true)
            Thread.sleep(50)
        }
    }
}