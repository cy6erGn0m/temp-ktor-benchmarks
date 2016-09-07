package org.jetbrains.ktor.benchmarks

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Benchmark(val concurrency: IntArray = intArrayOf(1, 10, 50, 100), val iterationsLimit: Int = 1000, val maxDurationMillis: Long = 10000, val minDurationMillis: Long = 1000)
