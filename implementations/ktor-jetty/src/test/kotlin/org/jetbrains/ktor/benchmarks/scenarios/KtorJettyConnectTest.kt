package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.benchmarks.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.routing.*
import org.junit.*
import org.junit.runner.*

@RunWith(ClientBenchmarkRunner::class)
class KtorJettyConnectTest : AbstractConnectBenchmark() {
    override val port: Int
        get() = server.port

    companion object {
        val server = object : AbstractKtorJettyBenchmark() {
            override fun createRoute(routing: Routing) {
                routing.apply {
                    get("/") {
                        call.respond("OK")
                    }
                }
            }
        }

        @BeforeClass
        @JvmStatic
        fun setUp() {
            server.setUp()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.teamDown()
        }
    }
}
