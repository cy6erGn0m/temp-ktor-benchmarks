package org.jetbrains.ktor.benchmarks.scenarios

import org.junit.*

abstract class AbstractBenchmark {
    abstract val port: Int

    @Before
    fun ensurePort() {
        require(port > 0) { "Port number was not assigned" }
    }
}