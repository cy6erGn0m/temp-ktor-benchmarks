package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.runner.*
import org.junit.runners.model.*

internal class FightTestStatement(testClass: TestClass, child: FrameworkMethod, description: Description, val benchmarkAnnotation: Benchmark, collector: (Timer) -> Unit) : BaseTestStatement(testClass, child, description, collector) {

    override fun mainStatement(next: Statement, instance: AbstractBenchmark): Statement = FightStatement(child, next, instance, benchmarkAnnotation)
}