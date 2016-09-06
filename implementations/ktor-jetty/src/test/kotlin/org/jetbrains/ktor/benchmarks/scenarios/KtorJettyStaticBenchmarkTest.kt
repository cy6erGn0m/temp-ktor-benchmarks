package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.benchmarks.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.routing.*
import org.junit.runner.*

@RunWith(ClientBenchmarkRunner::class)
class KtorJettyStaticBenchmarkTest : AbstractStaticBenchmark(), AbstractKtorJettyBenchmark {
    override var server: ApplicationHostStartable? = null

    override fun createRoute(routing: Routing) {
        routing.apply {
            get("/static.txt") {
                call.respond("OK")
            }
            get("/localFile") {
                call.respond(call.resolveClasspathWithPath("", "/logback.xml")!!)
            }
        }
    }
}