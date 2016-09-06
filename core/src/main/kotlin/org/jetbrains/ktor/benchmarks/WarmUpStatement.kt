package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.runners.model.*

internal class WarmUpStatement(val child: FrameworkMethod, val next: Statement, val instance: AbstractBenchmark) : Statement() {
    override fun evaluate() {
        for (i in 1..10) {
            child.invokeExplosively(instance)
        }
        next.evaluate()
    }
}