package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.benchmarks.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.jetty.*
import org.jetbrains.ktor.routing.*
import org.junit.runner.*

@RunWith(ClientBenchmarkRunner::class)
class KtorJettyStaticBenchmarkTest : AbstractStaticBenchmark() {
    var server: ApplicationHost? = null

    override fun start() {
        server = embeddedJettyServer(port) {
            get("/static.txt") {
                call.respond("OK")
            }
            get("/localFile") {
                call.respond(call.resolveClasspathWithPath("", "/logback.xml")!!)
            }
        }.apply {
            start(false)
        }
    }

    override fun stop() {
        (server as? ApplicationHostStartable)?.stop()
    }
}