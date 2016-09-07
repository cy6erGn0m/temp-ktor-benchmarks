package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.runners.model.*

class FightStatement(val child: FrameworkMethod, val next: Statement, val instance: AbstractBenchmark, val benchmarkAnnotation: Benchmark) : Statement() {
    override fun evaluate() {
        val maxCount = benchmarkAnnotation.iterationsLimit
        val minCount = Math.min(100, maxCount)
        val maxDurationMillis = benchmarkAnnotation.maxDurationMillis
        val minDurationMills = benchmarkAnnotation.minDurationMillis

        var count = 0
        val start = System.currentTimeMillis()

        while (count < maxCount && (System.currentTimeMillis() - start) < maxDurationMillis
                && (count < minCount || ((System.currentTimeMillis() - start) < minDurationMills))) {

            count++
            child.invokeExplosively(instance)
        }

        next.evaluate()
    }
}
