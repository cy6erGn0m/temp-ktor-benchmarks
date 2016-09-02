package org.jetbrains.ktor.benchmarks

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Benchmark(val concurrency: Int = 10, val iteration: Int = 1000, val maxDurationMillis: Long = Long.MAX_VALUE)
