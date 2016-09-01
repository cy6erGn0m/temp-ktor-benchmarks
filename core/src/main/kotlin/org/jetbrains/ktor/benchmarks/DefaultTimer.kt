package org.jetbrains.ktor.benchmarks

import java.time.*
import java.util.*
import java.util.concurrent.atomic.*

class DefaultTimer : Timer {
    private val recorded = ArrayList<DefaultMeasurement>()

    override val measurements: List<Timer.Measurement>
        get() = recorded

    override fun start(label: String) = DefaultMeasurement(label).apply { recorded.add(this) }

    class DefaultMeasurement(override val label: String) : Timer.Measurement {
        private val allLaps = ArrayList<Lap>()
        private val stopLap = AtomicReference<Lap?>()

        override val start: Long = System.nanoTime()

        override val laps: List<Timer.Measurement.Lap>
            get() = allLaps

        override fun lap(label: String) {
            val time = System.nanoTime()
            allLaps.add(Lap(time, label))
        }

        override fun ensureStopped(): Boolean {
            val time = System.nanoTime()

            if (stopLap.get() == null) {
                if (stopLap.compareAndSet(null, Lap(time, "Stop"))) {
                    allLaps.add(stopLap.get()!!)
                    return true
                }
            }

            return false
        }

        override fun stop() {
            if (!ensureStopped()) {
                throw IllegalStateException("Already stopped")
            }
        }

        inner class Lap(override val timestamp: Long, override val label: String): Timer.Measurement.Lap {

            override val fromStart: Duration
                get() = Duration.ofNanos(timestamp - start)
        }
    }
}