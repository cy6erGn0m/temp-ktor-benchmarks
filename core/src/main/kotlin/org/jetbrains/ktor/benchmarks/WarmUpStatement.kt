package org.jetbrains.ktor.benchmarks

import org.junit.runners.model.*
import java.lang.invoke.*

internal class WarmUpStatement(val method: MethodHandle, val next: Statement) : Statement() {
    override fun evaluate() {
        for (i in 1..10) {
            method.invokeWithArguments()
        }
        next.evaluate()
    }
}