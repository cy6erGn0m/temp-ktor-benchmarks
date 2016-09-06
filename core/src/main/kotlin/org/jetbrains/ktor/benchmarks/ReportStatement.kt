package org.jetbrains.ktor.benchmarks

import org.junit.runners.model.*
import java.io.*
import java.math.*
import java.time.*

class ReportStatement(val testClass: TestClass, val child: FrameworkMethod, val results: Map<Int, Map<String, List<Timer.Measurement>>>) : Statement() {
    override fun evaluate() {
        val allMeasurements = results.values.flatMap { it.values.flatMap { it } }

        val groupedBy = allMeasurements.filter { it.laps.isNotEmpty() }.groupBy { it.label }
        val metrics = groupedBy.mapValues { it.value.toMetric() }

        println("Test ${child.name} completed")
        val maxLabelLength = metrics.keys.maxBy { it.length }?.length ?: 0

        metrics.keys.sorted().forEach { name ->
            val m = metrics[name]!!

            println("    ${m.label.padEnd(maxLabelLength)} (${m.points} pts) ${m.avg.prettyPrint()} (${m.min.prettyPrint()} .. ${m.max.prettyPrint()})")
        }

        val allLabels = results.values.flatMap { it.values.flatMap { it.map { it.label } } }.distinct()

        for (m in allLabels) {
            val filtered = results.entries.map { e -> e.key to (e.value[m] ?: emptyList()) }

            val xPoints = filtered.flatMap { pair -> pair.second.filter { it.laps.isNotEmpty()}.map { pair.first.toDouble() } }.toDoubleArray()
            val yPoints = filtered.flatMap { pair -> pair.second.map { it.laps.last().fromStart.toMillisExact().toDouble() } }.toDoubleArray()

            val avXPoints = filtered.map { it.first.toDouble() }.toDoubleArray()
            val avYPoints = filtered.map { it.second.mapNotNull { it.laps.lastOrNull()?.fromStart?.toMillisExact()?.toDouble() }.average() }.toDoubleArray()

            val reportData = ReportData(
                    testClass.name,
                    child.declaringClass.name + "." + child.name,
                    m,
                    xPoints, yPoints,
                    avXPoints, avYPoints)

            val reportDataFile = reportDataFile(child, m, testClass)
            reportDataFile.parentFile.mkdirs()
            reportDataFile.outputStream().let(::ObjectOutputStream).use { os ->
                os.writeObject(reportData)
            }

            val chart = fillChart("${child.name} $m", listOf(reportData))

            val chartFile = File("target/charts/${safePathComponent(child.name)}/${safePathComponent(m)}.png")
            chartFile.parentFile.mkdirs()

            renderChart(chart, chartFile)
        }
    }

    private fun Duration.toMillisExact() = BigDecimal.valueOf(toMillis()) + BigDecimal.valueOf(nano.toLong()).divide(BigDecimal.valueOf(1000000L))

    private fun List<Timer.Measurement>.toMetric(): ClientBenchmarkRunner.Metric {
        if (isEmpty()) {
            throw IllegalArgumentException("No measurements found")
        }

        val times = LongArray(size) { idx -> this[idx].laps.last().fromStart.toNanos() }

        val min = times.min()!!
        val max = times.max()!!
        val avg = times.map { BigInteger.valueOf(it)!! }.reduce(BigInteger::plus).div(BigInteger.valueOf(size.toLong())).longValueExact()

        return ClientBenchmarkRunner.Metric(first().label, Duration.ofNanos(min), Duration.ofNanos(max), Duration.ofNanos(avg), times.size)
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
}