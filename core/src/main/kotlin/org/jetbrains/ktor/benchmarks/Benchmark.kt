package org.jetbrains.ktor.benchmarks

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Benchmark(val concurrency: Int = 10, val iterationsLimit: Int = 1000, val maxDurationMillis: Long = 10000, val minDurationMillis: Long = 1000)
