package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.runner.*
import org.junit.runners.model.*
import java.lang.invoke.*

internal class FightTestStatement(testClass: TestClass, methodNotBound: MethodHandle, child: FrameworkMethod, description: Description, val benchmarkAnnotation: Benchmark, collector: (Timer) -> Unit) : BaseTestStatement(testClass, child, methodNotBound, description, collector) {

    override fun mainStatement(next: Statement, method: MethodHandle, instance: AbstractBenchmark): Statement {
        return FightStatement(method, next, benchmarkAnnotation)
    }
}