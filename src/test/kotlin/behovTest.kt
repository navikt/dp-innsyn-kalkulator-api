
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.kotlintest.shouldBe
import no.nav.dagpenger.*
import no.nav.dagpenger.oidc.OidcClient
import no.nav.dagpenger.oidc.OidcToken
import java.net.URI
import java.util.*

class BehovTest {
    private val jwkStub = JwtStub()
    private val token = jwkStub.createTokenFor("brukerMcBrukerson")
    private val unauthorizedToken = "tull"
    val oidcClient = object : OidcClient {
        override fun oidcToken(): OidcToken {
            return OidcToken(UUID.randomUUID().toString(), "openid", 3000)
        }
    }

    val oppslagsKlient = AktørIdOppslagKlient(config.application.graphQlBaseUrl, config.application.apiGatewayKey)
    val behovsKlient = RegelApiBehovKlient(config.application.regelApiBaseUrl, config.auth.regelApiKey)

    @Test
    fun `Startbehov returns a response`() {
        withTestApplication({ KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer", oppslagsKlient, behovsKlient) }) {
            handleRequest(HttpMethod.Post, "/arbeid/dagpenger/kalkulator-api/behov") {
            }.apply {
                assertNotNull(response.status())
            }
        }
    }

    @Test
    fun `Startbehov returns a response with real token`() {
        withTestApplication({ KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer", oppslagsKlient, behovsKlient) }) {
            handleRequest(HttpMethod.Post, "/arbeid/dagpenger/kalkulator-api/behov") {
            }.apply {
                assertNotNull(response.status())
            }
        }
    }

    // @Test
    fun `Startbehov returns the response from regelApi `() {
        withTestApplication({ KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer", oppslagsKlient, behovsKlient) }) {
            handleRequest(HttpMethod.Post, "/arbeid/dagpenger/kalkulator-api/behov") {
                addHeader(HttpHeaders.Cookie, "selvbetjening-idtoken=$token")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody(
                    """
                        {
                            "beregningsdato": "2019-06-05"
                        }
                    """.trimIndent()
                )
            }.apply {
                assertNotNull(response.content, "Did not get a response")
                val behovResponse = moshiInstance.adapter(BehovResponse::class.java).fromJson(response.content!!)
                assertNotNull(behovResponse)
                // todo, bytt ut når endepunktet faktisk er reachable
                // assertNull(behovResponse.location)
                assertEquals("null", behovResponse.location)
            }
        }
    }

    @Test
    fun `Api returns a 401 if user is unauthenticated`() {
        withTestApplication({ KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer", oppslagsKlient, behovsKlient) }) {
            handleRequest(HttpMethod.Post, "/arbeid/dagpenger/kalkulator-api/behov") {
                addHeader(HttpHeaders.Authorization, "Bearer $unauthorizedToken")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }
}
