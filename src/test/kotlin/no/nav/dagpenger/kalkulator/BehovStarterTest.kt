package no.nav.dagpenger.kalkulator

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehovStarterTest {

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
    fun ` Should get url to behov status `() {

        val equalToPattern = EqualToPattern("Bearer testToken")
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("//behov"))
                .withHeader("Authorization", equalToPattern)
                .withRequestBody(
                    EqualToJsonPattern(
                        """
                    {
                        "aktorId": "001",
                        "beregningsdato": "${LocalDate.now()}"
                    }
                        """.trimIndent(),
                        true,
                        true
                    )
                )
                .willReturn(
                    WireMock.aResponse()
                        .withBody(responseBody)
                        .withHeader("Location", "/behov/status/123")
                )
        )

        val client = BehovStarter(server.url(""), tokenProviderMock)

        val response = runBlocking { client.startBehov("001", "corona") }
        Assertions.assertEquals("/behov/status/123", response)
    }

    private val responseBody =
        """
                {
                        "status" : "PENDING"
                }
        """.trimIndent()
}
