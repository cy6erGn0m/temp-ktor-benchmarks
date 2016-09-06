package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.client.*
import java.util.concurrent.*

class TimerAwareHttpClientWrapper(private val timer: Timer, private val delegate: HttpClient) : HttpClient {
    override fun openConnection(host: String, port: Int, secure: Boolean): HttpConnection
            = TimerAwareHttpConnectionWrapper(timer, delegate.openConnection(host, port, secure))

    private class TimerAwareHttpConnectionWrapper(val timer: Timer, val delegate: HttpConnection) : HttpConnection by delegate {
        private var measurement: Timer.Measurement? = null

        override fun requestBlocking(init: RequestBuilder.() -> Unit): HttpResponse {
            val response = delegate.requestBlocking {
                init()
                measurement = timer.start("${method.value} $path")
            }
            measurement?.lap("response")
            return response
        }

        override fun requestAsync(init: RequestBuilder.() -> Unit, handler: (Future<HttpResponse>) -> Unit) {
            delegate.requestAsync({
                init()
                measurement = timer.start("${method.value} $path")
            }, { f ->
                measurement?.lap("response")
                handler(f)
            })
        }

        override fun close() {
            measurement?.end()
            delegate.close()
        }
    }
}
