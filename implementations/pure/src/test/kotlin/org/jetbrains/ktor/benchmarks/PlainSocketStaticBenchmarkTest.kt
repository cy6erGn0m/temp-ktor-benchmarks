package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.benchmarks.scenarios.*
import org.junit.*
import org.junit.runner.*
import java.io.*
import java.net.*
import java.util.concurrent.*
import kotlin.concurrent.*

@RunWith(ClientBenchmarkRunner::class)
class PlainSocketStaticBenchmarkTest : AbstractStaticBenchmark() {
    override val port: Int
        get() = server.port

    companion object {
        @JvmStatic
        @get:ClassRule
        val server = object : AbstractBenchmarkServer() {
            private val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)
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
                                soTimeout = 10000

                                pool.submit {
                                    try {
                                        handleClient(this)
                                    } finally {
                                        close()
                                    }
                                }
                            }
                        } catch (ignore: IOException) {
                        }
                    } while (!Thread.interrupted())
                }
            }

            override fun stopImpl() {
                socket?.close()
                acceptor?.interrupt()
                pool.shutdownNow()
            }

            private fun handleClient(client: Socket) {
                client.inputStream.reader(Charsets.ISO_8859_1).buffered().use { request ->
                    client.outputStream.writer(Charsets.ISO_8859_1).buffered().use { response ->
                        val seq = request.lineSequence()
                        /*val headers = */seq.takeWhile(String::isNotBlank)

                        response.appendln("HTTP/1.1 200 OK")
                        response.appendln("Content-Type: text/plain;charset=iso-8859-1")
                        response.appendln("Content-Length: 3")
                        response.appendln()
                        response.write("OK\n")
                        response.flush()
                    }
                }
            }
        }
    }

}