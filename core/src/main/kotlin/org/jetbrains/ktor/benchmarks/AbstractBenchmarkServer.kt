package org.jetbrains.ktor.benchmarks

import org.junit.rules.*
import org.junit.runner.*
import org.junit.runners.model.*
import java.io.*
import java.net.*

abstract class AbstractBenchmarkServer : TestRule {
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

    override fun apply(base: Statement, description: Description): Statement {
        if (description.isSuite) {
            return object : Statement() {
                override fun evaluate() {
                    setUp()
                    try {
                        base.evaluate()
                    } finally {
                        teamDown()
                    }
                }
            }
        } else {
            return base
        }
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