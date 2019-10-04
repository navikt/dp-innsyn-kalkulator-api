
import com.auth0.jwk.JwkProvider
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import io.kotlintest.shouldBe
import io.ktor.config.MapApplicationConfig
import java.net.URI

class BehovTest {
    private val jwkStub = JwtStub()
    private val token = jwkStub.createTokenFor("brukerMcBrukerson")
    private val unauthorizedToken = "tull"

    @Test
    fun `Startbehov returns a response`() {
        withTestApplication({ KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer") }) {
            handleRequest(HttpMethod.Post, "/behov") {
            }.apply {
                assertNotNull(response.status())
            }
        }
    }

    @Test
    fun `Startbehov returns the response from regelApi `() {
        withTestApplication({ KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer")  }) {
            handleRequest(HttpMethod.Post, "/behov") {
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
                assertEquals("test", behovResponse.location)
            }
        }
    }

    @Test
    fun `startBehov returns error if json is missing beregningsdato`() {
        withTestApplication(
                {
                    KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer")  }) {
            handleRequest(HttpMethod.Post, "/behov") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.Cookie, "selvbetjening-idtoken=$token")
                setBody(
                        """
                            {}
                    """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                moshiInstance.adapter<Problem>(Problem::class.java).fromJson(response.content!!).apply {
                    this?.type shouldBe URI("urn:dp:error:parameter")
                }
            }
        }
    }

    @Test
    fun `Api returns a 401 if user is unauthenticated`() {
        withTestApplication({ KalkulatorDings(jwkStub.stubbedJwkProvider(), "test issuer") }) {
            handleRequest(HttpMethod.Post, "/behov"){
                addHeader(HttpHeaders.Authorization, "Bearer $unauthorizedToken")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }
}
