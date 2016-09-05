package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.junit.*
import java.net.*

abstract class AbstractConnectBenchmark : AbstractBenchmark() {
    @get:Rule
    val timer = TimerRule()

    @Test
    @Benchmark(iterationsLimit = 1000)
    fun simpleConnect() {
        val m = timer.start("Connect")
        val socket = Socket("localhost", port)
        m.end()
        socket.close()
    }
}