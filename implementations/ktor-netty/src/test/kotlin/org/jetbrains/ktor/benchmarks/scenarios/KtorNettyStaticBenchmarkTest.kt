package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.jetbrains.ktor.routing.*
import org.junit.*
import org.junit.runner.*

@RunWith(ClientBenchmarkRunner::class)
class KtorNettyStaticBenchmarkTest : KtorStaticBenchmark() {
    override val port: Int
        get() = server.port

    companion object {
        val server = object : AbstractKtorNettyBenchmark() {
            override fun createRoute(routing: Routing) {
                KtorStaticBenchmark.createRoute(routing)
            }
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            server.setUp()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.teamDown()
        }
    }
}
