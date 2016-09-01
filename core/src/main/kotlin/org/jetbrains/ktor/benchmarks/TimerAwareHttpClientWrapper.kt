package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.auth.httpclient.*

class TimerAwareHttpClientWrapper(val timer: Timer, val delegate: HttpClient) : HttpClient {
    override fun openConnection(host: String, port: Int, secure: Boolean): HttpConnection
            = TimerAwareHttpConnectionWrapper(timer, delegate.openConnection(host, port, secure))

    private class TimerAwareHttpConnectionWrapper(val timer: Timer, val delegate: HttpConnection) : HttpConnection by delegate {
        private var measurement: Timer.Measurement? = null

        override fun request(init: RequestBuilder.() -> Unit) {
            delegate.request {
                init()
                measurement = timer.start("${method.value} $path")
            }
            measurement?.lap("response")
        }
    }
}
