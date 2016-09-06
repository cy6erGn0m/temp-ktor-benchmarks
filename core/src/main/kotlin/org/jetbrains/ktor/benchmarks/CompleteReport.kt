package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.util.*
import org.junit.runners.model.*
import org.knowm.xchart.*
import org.knowm.xchart.style.*
import java.awt.*
import java.awt.image.*
import java.io.*
import java.util.*
import javax.imageio.*

fun generateFullReport(dir: File) {
    val reportDataDirs = dir.walkTopDown()
            .onEnter { it == dir || (it.name != "report-data" && it.name != "src" && !it.name.startsWith(".") && it.name != "classes" && it.name != "test-classes") }
            .maxDepth(10)
            .asSequence()
            .filter { it.isDirectory && "report-data" in it.list().orEmpty() }
            .filter { it.name == "benchmark" && it.parentFile.name == "target" }
            .map { File(it, "report-data") }
            .toList()

    val dirsMapping = HashMap<String, MutableList<File>>()
    for (reportDataDir in reportDataDirs) {
        for (testClassesDir in reportDataDir.listFiles(FileFilter { it.isDirectory && !it.name.startsWith(".") })) {
            for (testNameDir in testClassesDir.listFiles(FileFilter { it.isDirectory && !it.name.startsWith(".") })) {
                for (measurementDir in testNameDir.listFiles(FileFilter { it.isDirectory && !it.name.startsWith(".") })) {
                    println("Looking in $measurementDir")

                    val reportData = measurementDir.listFiles()
                            .orEmpty()
                            .filter { it.isFile && it.extension == "dat" }
                            .take(1)
                            .let(::readReportData)

                    if (reportData.isNotEmpty()) {
                        val some = reportData.first()

                        val list = dirsMapping.getOrPut(some.testFqName + "/" + some.measurementName) { ArrayList() }
                        list.add(measurementDir)
                    }
                }
            }
        }
    }

    for (measurementDir in dirsMapping.values) {
        val reportData = measurementDir.flatMap { it.listFiles().orEmpty().filter { it.isFile && it.extension == "dat" } }.let(::readReportData)

        if (reportData.isNotEmpty()) {
            val some = reportData.first()
            val chartFile = File("target/charts/summary/" + safePathComponent(some.testFqName) + "/" + safePathComponent(some.measurementName) + ".png")
            println("Generating ${chartFile.absolutePath}")

            val chart = fillChart("${some.testFqName.substringAfterLast('.')} ${some.measurementName}", reportData)
            chartFile.parentFile.mkdirs()
            renderChart(chart, chartFile)
        }
    }
}

private fun readReportData(files: List<File>) = files.map { f ->
    f.inputStream()
            .buffered()
            .let(::ObjectInputStream)
            .use(ObjectInputStream::readObject)
            .cast<ReportData>()
}

fun main(args: Array<String>) {
    generateFullReport(File("."))
}

fun fillChart(title: String, lines: List<ReportData>): XYChart {
    val chart = XYChartBuilder()
            .width(800).height(600)
            .title(title)
            .xAxisTitle("Concurrency").yAxisTitle("Time, ms")
            .build()

    chart.styler.defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Scatter
    chart.styler.markerSize = 8
    chart.styler.isYAxisLogarithmic = true
    chart.styler.legendPosition = Styler.LegendPosition.InsideNW

    for (data in lines) {
        chart.addSeries(data.testClassFqName, data.xPoints, data.yPoints)
        val averageLine = chart.addSeries(data.testClassFqName + " avg", data.avXPoints, data.avYPoints)

        averageLine.xySeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Line
    }

    return chart
}

fun renderChart(chart: XYChart, file: File) {
    val image =
            if (GraphicsEnvironment.isHeadless()) BufferedImage(800, 600, BufferedImage.TYPE_4BYTE_ABGR)
            else GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.createCompatibleImage(800, 600, java.awt.Transparency.TRANSLUCENT)

    image.createGraphics().apply {
        chart.paint(this, 800, 600)
        dispose()
    }

    ImageIO.write(image, "PNG", file)
}

fun reportDataFileDir(child: FrameworkMethod, name: String) = File("target/benchmark/report-data/" + safePathComponent(child.method.declaringClass.name) + "/" + safePathComponent(child.name) + "/" + safePathComponent(name))

fun reportDataFile(child: FrameworkMethod, name: String, testClass: TestClass) = File(reportDataFileDir(child, name), safePathComponent(testClass.name) + ".dat")

fun safePathComponent(c: String) = c.replace("[^\\w\\d._-]+".toRegex(), " ").trim().replace("\\s+".toRegex(), " ")
