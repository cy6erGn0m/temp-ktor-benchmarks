package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.routing.*

interface AbstractKtorBenchmark : KtorBenchmark {
    fun createRoute(routing: Routing)

    fun createServer(port: Int): ApplicationHostStartable

    var server: ApplicationHostStartable?

    override fun start(port: Int) {
        server = createServer(port).apply {
            start(false)
        }
    }

    override fun stop() {
        server?.stop()
    }
}