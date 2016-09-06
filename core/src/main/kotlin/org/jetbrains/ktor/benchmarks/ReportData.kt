package org.jetbrains.ktor.benchmarks

import java.io.*

class ReportData(val testClassFqName: String,
                 val testFqName: String,
                 val measurementName: String,
                 val xPoints: DoubleArray,
                 val yPoints: DoubleArray,
                 val avXPoints: DoubleArray,
                 val avYPoints: DoubleArray) : Serializable
