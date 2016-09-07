package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.routing.*

abstract class KtorStaticBenchmark : AbstractStaticBenchmark() {
    companion object {
        fun createRoute(routing: Routing) {
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
}