package org.jetbrains.ktor.benchmarks

import org.junit.*
import org.junit.rules.*
import org.junit.runner.*
import org.junit.runner.notification.*
import org.junit.runners.*
import org.junit.runners.model.*
import java.math.*
import java.time.*
import kotlin.reflect.*

class ClientBenchmarkRunner(clazz: Class<*>) : ParentRunner<FrameworkMethod>(clazz) {

    override fun run(notifier: RunNotifier) {
        val warmUp = Description.createTestDescription(testClass.name, ":warmUp")
        val warmUpStatement = object : Statement() {
            override fun evaluate() {
                for (i in 1..100000) {
                    warmUpTimer(DefaultTimer())
                }
                for (i in 1..100000) {
                    warmUpTimer(DummyTimer)
                }

                for (m in children) {
                    runChildImpl(m, warmUp, true).evaluate()
                }
            }
        }

        runLeaf(warmUpStatement, warmUp, notifier)

        super.run(notifier)
    }

    override fun runChild(child: FrameworkMethod, notifier: RunNotifier) {
        val description = describeChild(child)

        try {
            runLeaf(runChildImpl(child, description, false), description, notifier)
        } catch (t: Throwable) {
            notifier.fireTestFailure(Failure(description, t))
        }
    }

    private fun runChildImpl(child: FrameworkMethod, description: Description, warmUp: Boolean): Statement {
        val instance = testClass.onlyConstructor.newInstance()

        val benchmarkAnnotation = child.getAnnotation(Benchmark::class.java) ?: defaultBenchmark

        val concurrency = benchmarkAnnotation.concurrency
        val maxCount = benchmarkAnnotation.iterationsLimit
        val minCount = Math.max(100, maxCount)
        val maxDuration = benchmarkAnnotation.maxDurationMillis

        testClass.annotatedMethods.filter { it.getAnnotation(Before::class.java) != null }.forEach { m ->
            m.invokeExplosively(instance)
        }

        val rules = findRules(instance)

        val runAll = object : Statement() {
            override fun evaluate() {
                val allTimers = if (warmUp) {
                    for (i in 1..10) {
                        child.invokeExplosively(instance)
                    }
                    emptyList()
                } else {
                    println("Running test ${child.name}")

                    var count = 0
                    val start = System.currentTimeMillis()

                    while (count < maxCount && (count < minCount || ((System.currentTimeMillis() - start) < maxDuration))) {
                        count++
                        child.invokeExplosively(instance)
                    }

                    rules.filterIsInstance<TimerRule>()
                }
                testClass.annotatedMethods.filter { it.getAnnotation(After::class.java) != null }.forEach { m ->
                    m.invokeExplosively(instance)
                }

                if (!warmUp) {
                    allTimers.forEach { timer -> timer.measurements.forEach { it.ensureStopped() } }

                    val groupedBy = allTimers.flatMap { it.measurements }.filter { it.laps.isNotEmpty() }.groupBy { it.label }
                    val metrics = groupedBy.mapValues { it.value.toMetric() }

                    println("Test ${child.name} completed")
                    val maxLabelLength = metrics.keys.maxBy { it.length }?.length ?: 0

                    metrics.keys.sorted().forEach { name ->
                        val m = metrics[name]!!

                        println("    ${m.label.padEnd(maxLabelLength)} ${m.avg.prettyPrint()} (${m.min.prettyPrint()} .. ${m.max.prettyPrint()})")
                    }
                }
            }
        }

        return rules.fold<TestRule, Statement>(runAll) { base, rule -> rule.apply(base, description) }
    }

    override fun getChildren(): MutableList<FrameworkMethod> = testClass.annotatedMethods.filter { it.getAnnotation(Test::class.java) != null }.toMutableList()

    override fun describeChild(child: FrameworkMethod): Description {
        return Description.createTestDescription(testClass.name, child.name)
    }

    data class Metric(val label: String, val min: Duration, val max: Duration, val avg: Duration)

    private fun findRules(instance: Any) = testClass.getAnnotatedFieldValues(instance, Rule::class.java, TestRule::class.java) +
            testClass.getAnnotatedMethodValues(instance, Rule::class.java, TestRule::class.java)

    private fun List<Timer.Measurement>.toMetric(): Metric {
        if (isEmpty()) {
            throw IllegalArgumentException("No measurements found")
        }

        val times = LongArray(size) { idx -> this[idx].laps.last().fromStart.toNanos() }

        val min = times.min()!!
        val max = times.max()!!
        val avg = times.map { BigInteger.valueOf(it)!! }.reduce(BigInteger::plus).div(BigInteger.valueOf(size.toLong())).longValueExact()

        return Metric(first().label, Duration.ofNanos(min), Duration.ofNanos(max), Duration.ofNanos(avg))
    }

    private fun Duration.prettyPrint(): String {
        val millis = nano.toLong() / 1000000L
        val nanos = nano.toLong() % 1000000L

        return listOf(seconds to "s", millis to "ms", nanos to "ns").filter { it.first != 0L }.joinToString(" ") { "${it.first} ${it.second}" }
    }

    private fun warmUpTimer(timer: Timer) {
        timer.start("").apply {
            lap("")
            end()
            ensureStopped()
            laps.firstOrNull()
        }
        timer.measurements.firstOrNull()
    }

    companion object {
        private val defaultBenchmark by lazy { ClientBenchmarkRunner.Companion::class.functions.flatMap { it.annotations }.filterIsInstance<Benchmark>().single() }

        @Benchmark // we use it to get annotation default values
        @Suppress("UNUSED")
        private fun dummyForBenchmark() {
        }
    }
}