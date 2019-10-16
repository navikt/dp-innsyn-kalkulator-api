import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern
import no.nav.dagpenger.oidc.OidcClient
import no.nav.dagpenger.oidc.OidcToken
import no.nav.dagpenger.oidc.StsOidcClientException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AktorRegisterClientTest {

    val validResponse = """{
        |"aktørId": "12345"
        |}""".trimMargin()

    val oidcClient = object : OidcClient {
        override fun oidcToken(): OidcToken {
            return OidcToken(UUID.randomUUID().toString(), "openid", 3000)
        }
    }

    val failingOidcClient = object : OidcClient {
        override fun oidcToken(): OidcToken {
            throw StsOidcClientException("Failed!", RuntimeException("arrgg!"))
        }
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

    @Test
    fun `Client returns aktorid from jwk`() {
        val oppslagClient = no.nav.dagpenger.AktørIdOppslag(server.url(""), "", oidcClient)
        WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("//aktoer-ident"))
                        .withHeader("Authorization", RegexPattern("Bearer\\s[\\d|a-f]{8}-([\\d|a-f]{4}-){3}[\\d|a-f]{12}"))
                        .withHeader("ident", AnythingPattern())
                        .willReturn(WireMock.aResponse().withBody(validResponse))
        )

        val aktorid = oppslagClient.fetchAktørId("12345678910")
        assertEquals("12345", aktorid)
    }
}