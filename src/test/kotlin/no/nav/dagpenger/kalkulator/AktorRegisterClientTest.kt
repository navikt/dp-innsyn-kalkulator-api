package no.nav.dagpenger.kalkulator

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RegexPattern
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AktorRegisterClientTest {

    private val validResponse = Data(Person("12345"))

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

    @Test
    fun `Client returns aktorid from jwk`() {
        val body = AktorRegisterClientTest::class.java.getResource("/example-aktoerid-payload.json")
            .readText()
        val oppslagClient = AktørIdOppslagKlient(server.url(""), "key", "key")
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/"))
                .willReturn(WireMock.aResponse().withBody(body))
        )

        val responseBruker = oppslagClient.fetchAktørIdGraphql("12345678910", "tøken")
        assertEquals(validResponse.person, responseBruker)
    }

    @Test
    fun `håndterer 4xx-feil`() {

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/"))
                .withHeader("Authorization", RegexPattern("Bearer\\s[\\d|a-f]{8}-([\\d|a-f]{4}-){3}[\\d|a-f]{12}"))
                .willReturn(
                    WireMock.notFound()
                )
        )

        val oppslagClient = AktørIdOppslagKlient(server.url(""), "key", "key")

        val result = runCatching { oppslagClient.fetchAktørIdGraphql("-1", "token") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GraphQlAktørIdException)
    }
}
