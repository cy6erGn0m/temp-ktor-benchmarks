package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.netty.*

abstract class AbstractKtorNettyBenchmark : AbstractKtorBenchmarkServer() {
    override fun createServer(port: Int): ApplicationHostStartable {
        return embeddedNettyServer(port) {
            createRoute(this)
        }
    }
}