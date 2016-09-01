package org.jetbrains.ktor.benchmarks

import org.junit.runner.*
import org.junit.runner.notification.*
import org.junit.runners.*
import org.junit.runners.model.*
import java.math.*
import java.time.*
import java.util.*

open class ClientBenchmarkRunner(clazz: Class<*>) : ParentRunner<FrameworkMethod>(clazz) {
    private val instance = testClass.javaClass.newInstance()

    protected fun setupHost() {
        // TODO !!!
    }

    override fun run(notifier: RunNotifier) {
        for (i in 1..100000) {
            warmUpTimer(DefaultTimer())
        }
        for (i in 1..100000) {
            warmUpTimer(DummyTimer)
        }

        setupHost()



        super.run(notifier)
    }

    override fun runChild(child: FrameworkMethod, notifier: RunNotifier) {
        val description = describeChild(child)

        notifier.fireTestStarted(description)
        try {
            println("Warmup")
            for (i in 1..5) {
                child.invokeExplosively(instance, DummyTimer)
            }

            println("Running test ${child.name}")

            val allTimers = ArrayList<DefaultTimer>(1000)
            for (i in 1..1000) {
                val timer = DefaultTimer()
                val total = timer.start(child.name)
                child.invokeExplosively(instance, timer)
                total.stop()
                timer.measurements.forEach { it.ensureStopped() }

                allTimers.add(timer)
            }

            val groupedBy = allTimers.flatMap { it.measurements }.groupBy { it.label }
            val metrics = groupedBy.mapValues { it.value.toMetric() }

            println("Test ${child.name} completed")
            val maxLabelLength = metrics.keys.maxBy { it.length }?.length ?: 0

            metrics.keys.sorted().forEach { name ->
                val m = metrics[name]!!

                println("    ${m.label.padEnd(maxLabelLength)} ${m.avg.prettyPrint()} (${m.min.prettyPrint()} .. ${m.max.prettyPrint()})")
            }

            notifier.fireTestFinished(description)
        } catch (t: Throwable) {
            notifier.fireTestFailure(Failure(description, t))
        }
    }

    override fun getChildren(): MutableList<FrameworkMethod> = testClass.annotatedMethods.toMutableList()

    override fun describeChild(child: FrameworkMethod): Description {
        return Description.createTestDescription(testClass.name, child.name)
    }

    data class Metric(val label: String, val min: Duration, val max: Duration, val avg: Duration)

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
            stop()
            ensureStopped()
            laps.firstOrNull()
        }
        timer.measurements.firstOrNull()
    }
}