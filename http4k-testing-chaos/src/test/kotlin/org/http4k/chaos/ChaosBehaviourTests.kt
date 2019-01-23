package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
<<<<<<< HEAD

=======
>>>>>>> replace shouldMatch with assertThat
import com.natpryce.hamkrest.throws
import kotlinx.coroutines.runBlocking
import org.http4k.chaos.ChaosBehaviours.KillProcess
import org.http4k.chaos.ChaosBehaviours.Latency
import org.http4k.chaos.ChaosBehaviours.NoBody
import org.http4k.chaos.ChaosBehaviours.None
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosBehaviours.StackOverflow
import org.http4k.chaos.ChaosBehaviours.ThrowException
import org.http4k.chaos.ChaosBehaviours.Variable
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Jackson.asJsonObject
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration.ofMillis
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.concurrent.thread

private val request = Request(GET, "")
private val response = Response(OK).body("hello")

abstract class ChaosBehaviourContract {
    @Test
    abstract fun `deserialises from JSON`()
}

class ThrowExceptionBehaviourTest : ChaosBehaviourContract() {
    val description = "ThrowException RuntimeException foo"

    @Test
    fun `exception throwing behaviour should throw exception`() {
        val expected = RuntimeException("foo")
        val throwException = ThrowException(expected)
        assertThat(throwException.toString(), equalTo(description))

        assertThat({ runBlocking { throwException.then { response }(request) } }, throws(equalTo(expected)))
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"throw","message":"foo"}""".asJsonObject().asBehaviour()
        assertThat(behaviour.toString(), equalTo("ThrowException RuntimeException foo"))

        assertThat({ runBlocking { behaviour.then { response }(request) } }, throws<Exception>())
    }
}

class LatencyBehaviourTest : ChaosBehaviourContract() {
    val description = "Latency (range = PT0.1S to PT0.3S)"

    @Test
    fun `latency from env`() {
        val props = Properties().apply {
            put("CHAOS_LATENCY_MS_MIN", "100")
            put("CHAOS_LATENCY_MS_MAX", "300")
        }

        assertThat(Latency.fromEnv(props::getProperty).toString(), equalTo(description))
        assertThat(Latency.fromEnv().toString(), equalTo("Latency (range = PT0.1S to PT0.5S)"))
    }

    @Test
    override fun `deserialises from JSON`() = runBlocking {
        assertBehaviour("""{"type":"latency","min":"PT0.1S","max":"PT0.3S"}""",
            description,
            hasStatus(Status.OK).and(hasHeader("x-http4k-chaos", Regex("Latency.*"))))
    }

    @Test
    fun `latency behaviour should add extra latency`() {
        val delay = 100L
        val latency = Latency(ofMillis(delay), ofMillis(delay + 1))
        assertThat(latency.toString(), equalTo("Latency (range = PT0.1S to PT0.101S)"))

        val latch = CountDownLatch(1)
        thread {
            runBlocking { latency.then { response }(request) }
            latch.countDown()
        }
        assertThat(latch.await(delay - 1, MILLISECONDS), equalTo(false))
    }
}

class ReturnStatusBehaviourTest : ChaosBehaviourContract() {
    val description = "ReturnStatus (404)"

    @Test
    fun `should return response with internal server error status`() = runBlocking {
        val returnStatus: Behaviour = ReturnStatus(NOT_FOUND)
        assertThat(returnStatus.toString(), equalTo(description))

        val injectedResponse = returnStatus.then { response }(request)
        assertEquals(NOT_FOUND, injectedResponse.status)
    }

    @Test
    override fun `deserialises from JSON`() = runBlocking {
        assertBehaviour("""{"type":"status","status":404}""",
            description,
            hasStatus(NOT_FOUND.description("x-http4k-chaos")).and(hasHeader("x-http4k-chaos", Regex("Status 404"))))
    }
}

class NoBodyBehaviourTest : ChaosBehaviourContract() {
    val description = "NoBody"

    @Test
    fun `should return no body`() = runBlocking {
        val noBody = NoBody()
        assertThat(noBody.toString(), equalTo(description))

        assertThat(noBody.then { response }(request), hasHeader("x-http4k-chaos", "No body").and(hasBody("")))
<<<<<<< HEAD
=======
        Unit
>>>>>>> replace shouldMatch with assertThat
    }

    @Test
    override fun `deserialises from JSON`() = runBlocking {
        assertBehaviour("""{"type":"body"}""",
            description,
            hasStatus(OK).and(hasHeader("x-http4k-chaos", "No body")))
    }
}

class BlockThreadBehaviourTest : ChaosBehaviourContract() {
    val description = "BlockThread"

    @Test
    fun `should block thread`() = runBlocking {
        val blockThread = ChaosBehaviours.BlockThread()
        assertThat(blockThread.toString(), equalTo(description))
        val latch = CountDownLatch(1)
        thread {
            runBlocking { blockThread.then { response }(request) }
            latch.countDown()
        }

        assertThat(latch.await(100, MILLISECONDS), equalTo(false))
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"block"}""".asJsonObject().asBehaviour()
        assertThat(behaviour.toString(), equalTo(description))
    }
}

class EatMemoryBehaviourTest : ChaosBehaviourContract() {
    val description = "EatMemory"

    @Test
    fun `should eat memory`() {
        val eatMemory = ChaosBehaviours.EatMemory()
        assertThat(eatMemory.toString(), equalTo(description))

        assertThat({
            runBlocking { eatMemory.then { response }(request) }
        }, throws<OutOfMemoryError>())
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"memory"}""".asJsonObject().asBehaviour()
        assertThat(behaviour.toString(), equalTo(description))
    }
}

class DoNothingBehaviourTest : ChaosBehaviourContract() {
    private val description = "None"

    @Test
    fun `should do nothing memory`() = runBlocking {
        assertThat(None().toString(), equalTo(description))

        assertThat(None().then { response }(request), equalTo(response))
<<<<<<< HEAD
=======
        Unit
>>>>>>> replace shouldMatch with assertThat
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"none"}""".asJsonObject().asBehaviour()
        assertThat(behaviour.toString(), equalTo(description))
    }
}

class StackOverflowBehaviourTest : ChaosBehaviourContract() {
    private val description = "StackOverflow"

    @Test
    @Disabled // untestable
    fun `should stack overflow`() = runBlocking {
        val stackOverflow = StackOverflow()
        assertThat(stackOverflow.toString(), equalTo(description))
        stackOverflow.then { response }(request)
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"overflow"}""".asJsonObject().asBehaviour()
        assertThat(behaviour.toString(), equalTo(description))
    }
}

class KillProcessBehaviourTest : ChaosBehaviourContract() {

    private val description = "KillProcess"

    @Test
    @Disabled // untestable
    fun `should kill process`() = runBlocking {
        val killProcess = KillProcess()
        assertThat(killProcess.toString(), equalTo(description))
        killProcess.then { response }(request)
    }

    @Test
    override fun `deserialises from JSON`() {
        val behaviour = """{"type":"kill"}""".asJsonObject().asBehaviour()
        assertThat(behaviour.toString(), equalTo(description))
    }
}

class VariableBehaviourTest {
    @Test
    fun `should provide ability to modify behaviour at runtime`() = runBlocking {
        val variable = Variable()
        assertThat(variable.toString(), equalTo(("None")))
        assertThat(variable.then { response }(request), equalTo(response))
        variable.current = NoBody()
        assertThat(variable.toString(), equalTo(("NoBody")))
        assertThat(variable.then { response }(request), hasHeader("x-http4k-chaos", "No body").and(hasBody("")))
<<<<<<< HEAD
=======
        Unit
>>>>>>> replace shouldMatch with assertThat
    }
}