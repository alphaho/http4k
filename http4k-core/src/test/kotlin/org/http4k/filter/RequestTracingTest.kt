package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import kotlinx.coroutines.runBlocking
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.SamplingDecision.Companion.DO_NOT_SAMPLE
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestTracingTest {

    @BeforeEach
    fun before() {
        ZipkinTraces.THREAD_LOCAL.remove()
    }

    @Test
    fun `request traces are copied correctly from inbound to outbound requests`() = runBlocking {
        val originalTraceId = TraceId("originalTrace")
        val originalSpanId = TraceId("originalSpan")
        val originalParentSpanId = TraceId("originalParentSpanId")
        val traces = ZipkinTraces(originalTraceId, originalSpanId, originalParentSpanId, DO_NOT_SAMPLE)

        val client: HttpHandler = ClientFilters.RequestTracing().then {
            val actual = ZipkinTraces(it)

            assertThat(actual.traceId, equalTo(originalTraceId))
            assertThat(actual.parentSpanId, equalTo(originalSpanId))
            assertThat(actual.spanId, present())
            assertThat(actual.samplingDecision, equalTo(DO_NOT_SAMPLE))

            Response(OK)
        }

        val simpleProxyServer: HttpHandler = ServerFilters.RequestTracing().then { client(Request(Method.GET, "/somePath")) }

        val response = simpleProxyServer(ZipkinTraces(traces, Request(Method.GET, "")))

        assertThat(ZipkinTraces(response), equalTo(traces))
    }
}