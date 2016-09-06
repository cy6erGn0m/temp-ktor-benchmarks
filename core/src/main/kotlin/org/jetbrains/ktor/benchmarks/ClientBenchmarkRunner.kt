package org.jetbrains.ktor.benchmarks

import org.junit.*
import org.junit.rules.*
import org.junit.runner.*
import org.junit.runner.notification.*
import org.junit.runners.*
import org.junit.runners.model.*
import org.knowm.xchart.*
import org.knowm.xchart.style.*
import java.awt.*
import java.awt.image.*
import java.io.*
import java.math.*
import java.time.*
import javax.imageio.*
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
        val maxDurationMillis = benchmarkAnnotation.maxDurationMillis
        val minDurationMills = benchmarkAnnotation.minDurationMillis

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

                    while (count < maxCount && (System.currentTimeMillis() - start) < maxDurationMillis && (count < minCount || ((System.currentTimeMillis() - start) < minDurationMills))) {
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

                        println("    ${m.label.padEnd(maxLabelLength)} (${m.points} pts) ${m.avg.prettyPrint()} (${m.min.prettyPrint()} .. ${m.max.prettyPrint()})")
                    }

                    // render chart

                    for ((m, data) in groupedBy) {
                        val chart = XYChartBuilder()
                                .width(800).height(600)
                                .title("${child.name} $m")
                                .xAxisTitle("Concurrency").yAxisTitle("Time")
                                .build()

                        chart.styler.theme = GGPlot2Theme()
                        chart.styler.defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Scatter
                        chart.styler.markerSize = 8
                        chart.styler.isYAxisLogarithmic = true
//                        chart.styler.isLegendVisible = false

                        val yPoints = data.mapNotNull { it.laps.lastOrNull()?.fromStart?.toMillisExact()?.toDouble() }.toDoubleArray()
                        chart.addSeries(testClass.name.substringAfterLast('.'), yPoints.map { 1.0 }.toDoubleArray(), yPoints)

                        val image =
                                if (GraphicsEnvironment.isHeadless()) BufferedImage(800, 600, BufferedImage.TYPE_4BYTE_ABGR)
                                else GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.createCompatibleImage(800, 600, java.awt.Transparency.TRANSLUCENT)

                        image.createGraphics().apply {
                            chart.paint(this, 800, 600)
                            dispose()
                        }

                        val file = File("target/charts/${safePathComponent(child.name)}/${safePathComponent(m)}.png")
                        file.parentFile.mkdirs()

                        ImageIO.write(image, "PNG", file)
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

    data class Metric(val label: String, val min: Duration, val max: Duration, val avg: Duration, val points: Int)

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

        return Metric(first().label, Duration.ofNanos(min), Duration.ofNanos(max), Duration.ofNanos(avg), times.size)
    }

    private fun Duration.prettyPrint(): String {
        val millis = nano.toLong() / 1000000L
        val nanos = nano.toLong() % 1000000L

        val result = mutableListOf(seconds.toString() to "s")
        if (nanos < 1000) {
            result += millis.toString() to "ms"
            result += nanos.toString() to "ns"
        } else {
            val ms = BigDecimal.valueOf(millis) + BigDecimal.valueOf(nanos).divide(BigDecimal.valueOf(1000000L))
            result += ms.stripTrailingZeros().toPlainString() to "ms"
        }

        return result.filter { it.first != "0" }.joinToString(" ") { "${it.first} ${it.second}" }
    }

    private fun safePathComponent(c: String) = c.replace("[^\\w\\d]+".toRegex(), " ").trim().replace("\\s+".toRegex(), " ")

    private fun Duration.toMillisExact() = BigDecimal.valueOf(toMillis()) + BigDecimal.valueOf(nano.toLong()).divide(BigDecimal.valueOf(1000000L))

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