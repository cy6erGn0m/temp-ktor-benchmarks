package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.jetbrains.ktor.nio.*
import org.junit.*

abstract class AbstractStaticBenchmark : AbstractBenchmark() {
    @get:Rule
    val timer = TimerRule()

    @get:Rule
    val client = HttpClientRule(timer)

    @Test
    fun localStaticText() {
        val connection = client.openConnection("localhost", port)

        val indexPage = timer.start("DL /static.txt")
        val response = connection.requestBlocking {
            path = "/static.txt"
        }

        Assert.assertEquals(200, response.status.value)

        response.channel.asInputStream().readBytes()
        indexPage.end()

        connection.close()
    }

    @Test
    fun localFileContent() {
        val connection = client.openConnection("localhost", port)

        val indexPage = timer.start("DL /localFile")
        val response = connection.requestBlocking {
            path = "/localFile"
        }

        Assert.assertEquals(200, response.status.value)

        response.channel.asInputStream().readBytes()
        indexPage.end()

        connection.close()
    }
}
