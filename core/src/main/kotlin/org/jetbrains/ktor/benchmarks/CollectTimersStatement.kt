package org.jetbrains.ktor.benchmarks

import org.junit.*
import org.junit.rules.*
import org.junit.runners.model.*

class CollectTimersStatement<T>(val testClass: TestClass, val instance: Any, val collector: (Timer) -> T) : Statement() {
    override fun evaluate() {
        findRules<TimerRule>(instance).forEach { collector(it) }
    }

    private fun findAllRules(instance: Any) = testClass.getAnnotatedFieldValues(instance, Rule::class.java, TestRule::class.java) +
            testClass.getAnnotatedMethodValues(instance, Rule::class.java, TestRule::class.java)

    private inline fun <reified T> findRules(instance: Any) = findAllRules(instance).filterIsInstance<T>()
}