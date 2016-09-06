package org.jetbrains.ktor.benchmarks

interface KtorBenchmark {
    fun start(port: Int)
    fun stop()
}