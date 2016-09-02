package org.jetbrains.ktor.benchmarks

import org.junit.*
import org.junit.runner.*

@RunWith(ClientBenchmarkRunner::class)
class TestRunner {
    @get:Rule
    val index: ServerRule = ServerRule(DefaultTestApp)

    @Test
    fun smokeTest(timer: Timer) {
        Thread.sleep(1)
    }
}