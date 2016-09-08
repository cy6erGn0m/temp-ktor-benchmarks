package org.jetbrains.ktor.benchmarks.scenarios

import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.*
import org.jetbrains.ktor.benchmarks.*
import org.junit.*
import org.junit.runner.*
import javax.servlet.http.*

@RunWith(ClientBenchmarkRunner::class)
class PureJettyStaticBenchmarkTest : AbstractStaticBenchmark() {
    override val port: Int
        get() = server.port

    companion object {
        @JvmStatic
        @get:ClassRule
        val server = object : AbstractBenchmarkServer() {
            lateinit var jettyServer: Server

            override fun startImpl(port: Int) {
                jettyServer = Server(port).apply {
                    connectors.forEach { if (it is ServerConnector) it.soLingerTime = 50 }
                    handler = object : AbstractHandler() {
                        override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                            when (request.requestURI) {
                                "/static.txt" -> response.writer.apply { append("OK"); flush() }
                                "/localFile" -> {
                                    PureJettyStaticBenchmarkTest::class.java.classLoader.getResourceAsStream("logback.xml").copyTo(response.outputStream)
                                    response.outputStream.flush()
                                }
                                else -> response.sendError(HttpServletResponse.SC_NOT_FOUND)
                            }
                        }
                    }

                    start()
                }
            }

            override fun stopImpl() {
                jettyServer.stop()
            }
        }
    }
}