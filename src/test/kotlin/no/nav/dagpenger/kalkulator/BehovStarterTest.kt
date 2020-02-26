package no.nav.dagpenger.kalkulator

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.junit.jupiter.api.*
import java.time.LocalDate

class BehovStarterTest {

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
        val jsonAdapter = moshiInstance.adapter(BehovRequest::class.java)
        val json = jsonAdapter.toJson(BehovRequest(aktorId = "001", regelkontekst = RegelKontekst("-1337", Kontekst.VEILEDNING), beregningsdato = LocalDate.now(), vedtakId = -1337))
        print(json)
        val equalToPattern = EqualToPattern("regelApiKey")
        WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("//behov"))
                        .withHeader("X-API-KEY", equalToPattern)
                        .withRequestBody(EqualToJsonPattern("""
                            $json
                """.trimIndent(), true, true))
                        .willReturn(
                                WireMock.aResponse()
                                        .withBody(responseBody)
                                        .withHeader("Location", "/behov/status/123")
                        )
        )

        val client = BehovStarter(server.url(""), equalToPattern.value, "test")

        val response = client.startBehov("001")
        Assertions.assertEquals("/behov/status/123", response)
    }

    private val responseBody = """
                {
                        "status" : "PENDING"
                }
            """.trimIndent()
}
