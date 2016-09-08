package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.routing.*

abstract class KtorStaticBenchmark : AbstractStaticBenchmark() {
    companion object {
        var logbackObj: LocalFileContent? = null

        fun createRoute(routing: Routing) {
            routing.apply {
                get("/static.txt") {
                    call.respond("OK")
                }
                get("/localFile") {
                    val response = logbackObj ?: run {
                        logbackObj = call.resolveClasspathWithPath("", "/logback.xml") as LocalFileContent
                        logbackObj!!
                    }

                    call.respond(response)
                }
            }
        }
    }
}