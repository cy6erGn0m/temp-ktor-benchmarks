package org.jetbrains.ktor.benchmarks

import org.jetbrains.ktor.client.*
import org.junit.rules.*
import org.junit.runner.*
import org.junit.runners.model.*

class HttpClientRule private constructor(private val timer: Timer, val port: Int, realClient: HttpClient, private val client: HttpClient = TimerAwareHttpClientWrapper(timer, realClient)) : TestRule, HttpClient by client {

    constructor(timer: Timer, port: Int) : this(timer, port, DefaultHttpClient)

    override fun apply(base: Statement, description: Description): Statement = base
}