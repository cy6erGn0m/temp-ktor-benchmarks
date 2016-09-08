package org.jetbrains.ktor.benchmarks

import org.junit.runners.model.*
import java.lang.invoke.*

class FightStatement(val method: MethodHandle, val next: Statement, val benchmarkAnnotation: Benchmark) : Statement() {
    override fun evaluate() {
        val maxCount = benchmarkAnnotation.iterationsLimit
        val minCount = Math.min(1000, maxCount)
        val maxDurationMillis = benchmarkAnnotation.maxDurationMillis
        val minDurationMills = benchmarkAnnotation.minDurationMillis

        var count = 0
        val start = System.currentTimeMillis()

        while (count < maxCount && (System.currentTimeMillis() - start) < maxDurationMillis
                && (count < minCount || ((System.currentTimeMillis() - start) < minDurationMills))) {

            count++
            method.invokeWithArguments()
        }

        next.evaluate()
    }
}
