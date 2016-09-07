package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.*
import org.junit.internal.runners.statements.*
import org.junit.rules.*
import org.junit.runner.*
import org.junit.runners.model.*

internal abstract class BaseTestStatement(val testClass: TestClass, val child: FrameworkMethod, val description: Description, val collector: (Timer) -> Unit) : Statement() {
    final override fun evaluate() {
        System.gc()
        val instance = testClass.onlyConstructor.newInstance()
        if (instance !is AbstractBenchmark) {
            throw IllegalArgumentException("Test class should inherit AbstractBenchmark")
        }
        System.gc()

        val collect = CollectTimersStatement(testClass, instance, collector)
        val mainRun = mainStatement(collect, instance)

        val runBefore = RunBefores(mainRun, testClass.getAnnotatedMethods(Before::class.java), instance)
        val runAfter = RunAfters(runBefore, testClass.getAnnotatedMethods(After::class.java), instance)

        val rules = findAllRules(instance)

        rules.fold(runAfter as Statement) { next, rule -> rule.apply(next, description) }.evaluate()
    }

    protected abstract fun mainStatement(next: Statement, instance: AbstractBenchmark): Statement

    private fun findAllRules(instance: Any) = testClass.getAnnotatedFieldValues(instance, Rule::class.java, TestRule::class.java) +
            testClass.getAnnotatedMethodValues(instance, Rule::class.java, TestRule::class.java)
}