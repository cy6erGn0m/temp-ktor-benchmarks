package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.runner.*
import java.io.*
import java.net.*
import kotlin.concurrent.*

@RunWith(ClientBenchmarkRunner::class)
class PlainSocketConnectTest : AbstractConnectBenchmark() {
    private var socket: ServerSocket? = null
    private var acceptor: Thread? = null

    override fun start() {
        socket = ServerSocket(port)
        acceptor = thread {
            val socket = this.socket!!

            do {
                try {
                    socket.accept().close()
                } catch (ignore: IOException) {
                }
            } while (!Thread.interrupted())
        }
    }

    override fun stop() {
        socket?.close()
        acceptor?.interrupt()
    }
}
