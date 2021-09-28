package no.nav.dagpenger.kalkulator

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.stubbing.Scenario
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class BehovStatusPollerTest {

    private val tokenProviderMock = object : ClientCredentialsClient {
        override suspend fun getAccessToken(): String = "testToken"
    }

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

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
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `Should honor timeout `() {
        val client = BehovStatusPoller(
            regelApiUrl = server.url("/"),
            tokenProvider = tokenProviderMock,
            timeout = Duration.ZERO
        )
        assertThrows(
            RegelApiTimeoutException::class.java
        ) {
            runBlocking { client.pollStatus("/") }
        }
    }

    @Test
    fun ` Should retry query until response redirects to the result `() {
        val pattern = EqualToPattern("Bearer testToken")

        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("//behov/status/123"))
                .withHeader("Authorization", pattern)
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                    WireMock.aResponse()
                        .withBody(responseBody)
                )
                .willSetStateTo("First pending")
        )

        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("//behov/status/123"))
                .withHeader("Authorization", pattern)
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First pending")
                .willReturn(
                    WireMock.aResponse()
                        .withBody(responseBody)
                )
                .willSetStateTo("Second pending")
        )

        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("//behov/status/123"))
                .withHeader("Authorization", pattern)
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second pending")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(303)
                        .withHeader("Location", "54321")
                )
        )

        val client =
            BehovStatusPoller(regelApiUrl = server.url(""), tokenProvider = tokenProviderMock)

        val response = runBlocking { client.pollStatus("/behov/status/123") }
        assertEquals("54321", response)
    }

    private val responseBody =
        """
                {
                        "status" : "PENDING"
                }
        """.trimIndent()
}
