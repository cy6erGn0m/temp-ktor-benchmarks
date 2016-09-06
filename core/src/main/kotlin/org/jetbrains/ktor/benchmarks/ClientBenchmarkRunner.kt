package org.jetbrains.ktor.benchmarks

import org.junit.*
import org.junit.runner.*
import org.junit.runner.notification.*
import org.junit.runners.*
import org.junit.runners.model.*
import java.time.*
import java.util.concurrent.*
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
                    WarmUpTestStatement(testClass, m, warmUp, {}).evaluate()
                }
            }
        }

        runLeaf(warmUpStatement, warmUp, notifier)

        super.run(notifier)
    }

    override fun runChild(child: FrameworkMethod, notifier: RunNotifier) {
        val description = describeChild(child)
        runLeaf(runChildImpl(child, description), description, notifier)
    }

    private fun runChildImpl(child: FrameworkMethod, description: Description): Statement {
        val benchmarkAnnotation = child.getAnnotation(Benchmark::class.java) ?: defaultBenchmark
        val pool = Executors.newFixedThreadPool(benchmarkAnnotation.concurrency.max() ?: 1)

        return object : Statement() {
            override fun evaluate() {
                try {
                    println("Running test ${child.name}")
                    val results = benchmarkAnnotation.concurrency.sortedDescending().map { concurrency ->
                        val allTimersForConcurrency = CopyOnWriteArrayList<Timer>()

                        val singleRun = FightTestStatement(testClass, child, description, benchmarkAnnotation, { allTimersForConcurrency.add(it) })
                        val concurrentRun = ConcurrentStatement(singleRun, EmptyStatement, pool, concurrency)

                        concurrentRun.evaluate()
                        allTimersForConcurrency.forEach { timer -> timer.measurements.forEach { it.ensureStopped() } }
                        val groupedBy = allTimersForConcurrency.flatMap { it.measurements }.filter { it.laps.isNotEmpty() }.groupBy { it.label }

                        concurrency to groupedBy
                    }.toMap()

                    ReportStatement(testClass, child, results).evaluate()
                } finally {
                    pool.shutdownNow()
                }
            }
        }
    }

    override fun getChildren(): MutableList<FrameworkMethod> = testClass.annotatedMethods.filter { it.getAnnotation(Test::class.java) != null }.toMutableList()

    override fun describeChild(child: FrameworkMethod): Description {
        return Description.createTestDescription(testClass.name, child.name)
    }

    data class Metric(val label: String, val min: Duration, val max: Duration, val avg: Duration, val points: Int)

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