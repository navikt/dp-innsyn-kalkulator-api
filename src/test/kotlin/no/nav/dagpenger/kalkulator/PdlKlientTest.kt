package no.nav.dagpenger.kalkulator

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PdlKlientTest {

    private val tokenProviderMock = object : ClientCredentialsClient {
        override suspend fun getAccessToken(): String = "testToken"
    }

    companion object {
        val server = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun setup() {
        WireMock.configureFor(server.port())
    }

    private val callSetup = WireMock.post(WireMock.urlEqualTo("/"))
        .withHeader("Content-Type", EqualToPattern("application/json"))
        .withHeader("TEMA", EqualToPattern("DAG"))
        .withHeader("Authorization", EqualToPattern("Bearer testToken"))

    @Test
    fun `Happy path med riktig headers`() {
        val body = PdlKlientTest::class.java.getResource("/example-aktoerid-payload.json")
            .readText()
        val oppslagClient = PDLKlient(server.url(""), tokenProviderMock)
        WireMock.stubFor(
            callSetup
                .willReturn(
                    WireMock.aResponse().withBody(
                        body
                    )
                )
        )
        runBlocking {
            assertEquals("111111111", oppslagClient.fetchAktørIdGraphql("12345678910"))
        }
    }

    @Test
    fun `håndterer 4xx-feil`() {

        WireMock.stubFor(
            callSetup.willReturn(
                WireMock.notFound()
            )
        )
        val oppslagClient = PDLKlient(server.url(""), tokenProviderMock)

        assertFailsWith(PDLGraphQlAktørIdException::class) {
            runBlocking {
                oppslagClient.fetchAktørIdGraphql("-1")
            }
        }
    }

    @Test
    fun `håndterer 200 men med errors`() {

        val body = PdlKlientTest::class.java.getResource("/example-aktoerid-error-payload.json").readText()
        WireMock.stubFor(
            callSetup.willReturn(WireMock.aResponse().withBody(body))
        )
        val oppslagClient = PDLKlient(server.url(""), tokenProviderMock)

        assertFailsWith(PDLGraphQlAktørIdException::class) {
            runBlocking {
                oppslagClient.fetchAktørIdGraphql("-1")
            }
        }
    }
}
