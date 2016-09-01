package org.jetbrains.ktor.benchmarks

import java.time.*

interface Timer {
    val measurements: List<Measurement>
    fun start(label: String): Measurement

    interface Measurement {
        val label: String
        val start: Long
        val laps: List<Lap>

        fun lap(label: String)
        fun ensureStopped(): Boolean
        fun stop()

        interface Lap {
            val timestamp: Long
            val fromStart: Duration
            val label: String
        }
    }
}
