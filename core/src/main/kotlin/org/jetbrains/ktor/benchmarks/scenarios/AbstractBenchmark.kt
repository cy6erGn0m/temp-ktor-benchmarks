package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.junit.*
import java.io.*
import java.net.*

abstract class AbstractBenchmark : KtorBenchmark {
    @Volatile
    protected var port = 0

    @Before
    fun setUp() {
        do {
            try {
                port = findFreePort()
                start(port)
                waitForPort(port)
                break
            } catch (e: BindException) {
                stop()
            }
        } while (true)
    }

    @After
    fun teamDown() {
        stop()
    }

    companion object {
        fun findFreePort() = ServerSocket(0).use { it.reuseAddress = true; it.localPort }
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