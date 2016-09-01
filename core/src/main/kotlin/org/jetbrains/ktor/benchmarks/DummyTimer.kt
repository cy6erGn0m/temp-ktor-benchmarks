package org.jetbrains.ktor.benchmarks

object DummyTimer : Timer {
    override val measurements: List<Timer.Measurement>
        get() = emptyList()

    override fun start(label: String) = DummyMeasurement

    object DummyMeasurement : Timer.Measurement {
        override val label: String
            get() = "DUMMY"

        override val start: Long
            get() = 0

        override val laps: List<Timer.Measurement.Lap>
            get() = emptyList()

        override fun lap(label: String) {
        }

        override fun ensureStopped() = true

        override fun stop() {
        }
    }
}