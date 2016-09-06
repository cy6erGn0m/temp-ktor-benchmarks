package org.jetbrains.ktor.benchmarks

import org.junit.runners.model.*
import java.util.concurrent.*

class ConcurrentStatement(val statement: Statement, val next: Statement, val executorService: ExecutorService, val concurrency: Int) : Statement() {
    override fun evaluate() {
        val l = CountDownLatch(concurrency)
        val errors = CopyOnWriteArrayList<Throwable>()

        for (i in 1..concurrency) {
            executorService.submit {
                try {
                    statement.evaluate()
                } catch (t: Throwable) {
                    errors += t
                } finally {
                    l.countDown()
                }
            }
        }

        l.await()

        MultipleFailureException.assertEmpty(errors)
        next.evaluate()
    }
}