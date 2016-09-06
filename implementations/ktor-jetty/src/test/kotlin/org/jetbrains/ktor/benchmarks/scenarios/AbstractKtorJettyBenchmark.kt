package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.jetty.*

interface AbstractKtorJettyBenchmark : AbstractKtorBenchmark {
    override fun createServer(port: Int): ApplicationHostStartable {
        return embeddedJettyServer(port) {
            createRoute(this)
        }
    }
}