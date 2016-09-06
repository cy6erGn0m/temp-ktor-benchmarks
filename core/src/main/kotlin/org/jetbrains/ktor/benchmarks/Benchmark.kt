package org.jetbrains.ktor.benchmarks

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Benchmark(val concurrency: IntArray = intArrayOf(1), val iterationsLimit: Int = 100000, val maxDurationMillis: Long = 10000, val minDurationMillis: Long = 1000)
