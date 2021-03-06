package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.jetty.*

abstract class AbstractKtorJettyBenchmark : AbstractKtorBenchmarkServer() {
    override fun createServer(port: Int): ApplicationHostStartable {
        return embeddedJettyServer(port) {
            createRoute(this)
        }
    }
}