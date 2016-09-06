package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.junit.*
import java.io.*
import java.net.*

abstract class AbstractBenchmark : KtorBenchmark {
    protected var port = 0

    @Before
    fun setUp() {
        do {
            try {
                port = findFreePort()
                start(port)
                waitForPort(port)
            } catch (e: BindException) {
                stop()
                continue
            }
        } while (false)
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