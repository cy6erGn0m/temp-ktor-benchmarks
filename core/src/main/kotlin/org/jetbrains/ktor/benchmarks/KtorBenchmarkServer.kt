package org.jetbrains.ktor.benchmarks

import java.io.*
import java.net.*

abstract class KtorBenchmarkServer {
    @Volatile
    var port: Int = 0
        private set

    protected abstract fun startImpl(port: Int)
    protected abstract fun stopImpl()

    fun setUp() {
        do {
            try {
                port = findFreePort()
                startImpl(port)
                waitForPort(port)
                break
            } catch (e: BindException) {
                stopImpl()
            }
        } while (true)
    }

    fun teamDown() {
        stopImpl()
    }

    companion object {
        private fun findFreePort() = ServerSocket(0).use { it.reuseAddress = true; it.localPort }
        private fun waitForPort(port: Int) {
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