package org.jetbrains.ktor.benchmarks.scenarios

import org.jetbrains.ktor.benchmarks.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.routing.*

abstract class AbstractKtorBenchmarkServer : KtorBenchmarkServer() {
    protected abstract fun createRoute(routing: Routing)

    abstract fun createServer(port: Int): ApplicationHostStartable

    var server: ApplicationHostStartable? = null

    override fun startImpl(port: Int) {
        server = createServer(port).apply {
            start(false)
        }
    }

    override fun stopImpl() {
        server?.stop()
    }
}