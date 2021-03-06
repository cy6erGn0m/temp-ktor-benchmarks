package org.jetbrains.ktor.benchmarks

import org.junit.runners.model.*
import java.io.*
import java.math.*
import java.time.*

class ReportStatement(val testClass: TestClass, val child: FrameworkMethod, val results: Map<Int, Map<String, List<Timer.Measurement>>>, val renderCharts: Boolean = false) : Statement() {
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
            val filtered = results.entries.map { e -> e.key to (e.value[m]?.filter { it.laps.isNotEmpty() } ?: emptyList()) }

            val xPoints = filtered.flatMap { pair -> pair.second.map { pair.first.toDouble() } }.toDoubleArray()
            val yPoints = filtered.flatMap { pair -> pair.second.map { it.laps.last().fromStart.toMillisExact().toDouble() } }.toDoubleArray()

            val avXPoints = filtered.map { it.first.toDouble() }.toDoubleArray()
            val avYPoints = filtered.map { it.second.map { it.laps.last().fromStart.toMillisExact().toDouble() }.average() }.toDoubleArray()

            val throughputXPoints = avXPoints
            val throughputYPoints = filtered.map { pair ->
                val pts = pair.second

                val count = pts.size
                val startNanos = pts.minBy { it.start }!!.start
                val endNanos = pts.maxBy { it.laps.last().timestamp }!!.laps.last().timestamp

                (count * 1e9) / (endNanos - startNanos).toDouble()
            }.toDoubleArray()

            val reportData = ReportData(
                    testClass.name,
                    child.declaringClass.name + "." + child.name,
                    m,
                    xPoints, yPoints,
                    avXPoints, avYPoints,
                    throughputXPoints, throughputYPoints)

            val reportDataFile = reportDataFile(child, m, testClass)
            reportData.writeReportData(reportDataFile)

            buildString {
                appendln("$m throughput")

                for ((concurrency, pts) in filtered) {
                    if (pts.isNotEmpty()) {
                        val count = pts.size
                        val startNanos = pts.minBy { it.start }!!.start
                        val endNanos = pts.maxBy { it.laps.last().timestamp }!!.laps.last().timestamp

                        val th = (count * 1e9) / (endNanos - startNanos).toDouble()

                        appendln("${concurrency.toString().padStart(3, ' ')} $th ops/s")
                    }
                }
            }.let(::println)

            if (renderCharts) {
                val chart = fillChart("${child.name} $m", listOf(reportData))
                renderChart(chart, File("target/charts/${safePathComponent(child.name)}/${safePathComponent(m)}.png"))
            }
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