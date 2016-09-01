package org.jetbrains.ktor.benchmarks

import org.junit.*
import org.junit.runner.*

@RunWith(ClientBenchmarkRunner::class)
class TestRunner {
    @Test
    fun smokeTest(timer: Timer) {
        Thread.sleep(1)
    }
}