package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.*
import org.junit.runner.*
import java.io.*
import java.net.*
import kotlin.concurrent.*

@RunWith(ClientBenchmarkRunner::class)
class PlainSocketConnectTest : AbstractConnectBenchmark() {
    override val port: Int
        get() = server.port

    companion object {
        @JvmStatic
        @get:ClassRule
        val server = object : AbstractBenchmarkServer() {
            private var socket: ServerSocket? = null
            private var acceptor: Thread? = null

            override fun startImpl(port: Int) {
                socket = ServerSocket().apply {
                    bind(InetSocketAddress(port))
                }
                acceptor = thread {
                    val socket = this.socket!!

                    do {
                        try {
                            socket.accept().apply {
                                setSoLinger(true, 50)
                                close()
                            }
                        } catch (ignore: IOException) {
                        }
                    } while (!Thread.interrupted())
                }
            }

            override fun stopImpl() {
                socket?.close()
                acceptor?.interrupt()
            }
        }
    }
}
