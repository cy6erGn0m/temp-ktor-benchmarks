package org.jetbrains.ktor.benchmarks

import org.junit.runner.*
import org.junit.runners.model.*

@BenchmarkServer
class TestApp : ServerRule.ServerApplication {
    override val implementsApplications: List<ServerRule.ServerApplicationKey>
        get() = listOf(DefaultTestApp)

    override fun apply(base: Statement, description: Description): Statement {
        return object: Statement() {
            override fun evaluate() {
                println("Start app")
                try {
                    base.evaluate()
                } finally {
                    println("Stop app")
                }
            }
        }
    }
}