package org.jetbrains.ktor.benchmarks

import org.junit.rules.*
import org.junit.runner.*
import org.junit.runners.model.*

class ServerRule(val key: ServerApplicationKey) : TestRule {

    interface ServerApplication : TestRule {
        val implementsApplications: List<ServerApplicationKey>
        override fun apply(base: Statement, description: Description): Statement
    }

    open class ServerApplicationKey(val name: String)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (description.isTest) {
                    val apps = searchForApps()
                    println("Found ${apps.size} application implementations ${key.name}")

                    apps.forEach { clazz ->
                        val instance = clazz.newInstance()
                        instance.apply(base, Description.createSuiteDescription(clazz.simpleName)
//                                .apply { addChild(description) }
                        )
                                .evaluate()
                    }
                } else {
                    base.evaluate()
                }
            }
        }
    }

    private fun searchForApps() = ApplicationIndex.searchByKey(key)
}