package org.jetbrains.ktor.benchmarks

import org.junit.rules.*
import org.junit.runner.*
import org.junit.runners.model.*

class TimerRule(val timerInstance: Timer) : TestRule, Timer by timerInstance {
    constructor() : this(DefaultTimer())

    override fun apply(base: Statement, description: Description): Statement {
        return if (description.isTest) {
            object : Statement() {
                override fun evaluate() {
                    val theWholeTest = timerInstance.start(description.methodName)
                    base.evaluate()
                    theWholeTest.ensureStopped()
                }
            }
        } else {
            base
        }
    }
}
