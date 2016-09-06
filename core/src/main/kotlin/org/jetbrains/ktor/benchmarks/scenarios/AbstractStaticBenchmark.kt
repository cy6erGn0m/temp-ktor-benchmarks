package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.junit.*

abstract class AbstractStaticBenchmark : AbstractBenchmark() {
    @get:Rule
    val timer = TimerRule()

    @get:Rule
    val client = HttpClientRule(timer, port)

    @Test
    @Benchmark(maxDurationMillis = 2000)
    fun localStaticText() {
        val connection = client.openConnection("localhost", port)

        val indexPage = timer.start("DL /static.txt")
        connection.request {
            path = "/static.txt"
        }

        Assert.assertEquals(200, connection.responseStatus.value)

        connection.responseStream.readBytes()
        indexPage.end()

        connection.close()
    }

    @Test
    @Benchmark(maxDurationMillis = 2000)
    fun localFileContent() {
        val connection = client.openConnection("localhost", port)

        val indexPage = timer.start("DL /localFile")
        connection.request {
            path = "/localFile"
        }

        Assert.assertEquals(200, connection.responseStatus.value)

        connection.responseStream.readBytes()
        indexPage.end()

        connection.close()
    }
}
