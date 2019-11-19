import io.kotlintest.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.AktørIdOppslagKlient
import no.nav.dagpenger.BehovStarter
import no.nav.dagpenger.DagpengeKalkulator
import no.nav.dagpenger.KalkulatorApp
import no.nav.dagpenger.MinsteinntektResultat
import no.nav.dagpenger.PeriodeResultat
import no.nav.dagpenger.Person
import no.nav.dagpenger.SatsResultat
import no.nav.dagpenger.Subsumsjon
import no.nav.dagpenger.moshiInstance
import no.nav.dagpenger.regel.api.internal.BehovStatusPoller
import no.nav.dagpenger.regel.api.internal.SubsumsjonFetcher
import no.nav.dagpenger.regel.api.internal.models.Faktum
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KalkulatorApiTest {
    private val jwkStub = JwtStub()
    private val token = jwkStub.createTokenFor("brukerMcBrukerson")
    private val unauthorizedToken = "tull"

    val aktørIdOppslagKlient: AktørIdOppslagKlient = mockk()
    val behovStarter: BehovStarter = mockk()
    val behovStatusPoller: BehovStatusPoller = mockk()
    val subsumsjonFetcher: SubsumsjonFetcher = mockk()
    val dagpengeKalkulator = DagpengeKalkulator(behovStarter, behovStatusPoller, subsumsjonFetcher)

    @Test
    fun `Startbehov returns a response`() {
        withTestApplication({
            KalkulatorApp(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Get, "/arbeid/dagpenger/kalkulator-api/behov") {
            }.apply {
                assertNotNull(response.status())
            }
        }
    }

    @Test
    fun `Startbehov returns a response with real token`() {
        withTestApplication({
            KalkulatorApp(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Get, "/arbeid/dagpenger/kalkulator-api/behov") {
            }.apply {
                assertNotNull(response.status())
            }
        }
    }

    @Test
    fun `Skal kunne kalkulere dagpenge sats og periode, samt inngangsvilkår minste arbeidsinntekt `() {
        every {
            aktørIdOppslagKlient.fetchAktørIdGraphql(any(), any())
        } returns Person("1234")

        every {
            behovStarter.startBehov("1234")
        } returns "htto://localhost/1234"

        every {
            runBlocking { behovStatusPoller.pollStatus("htto://localhost/1234") }
        } returns "htto://localhost/1234"

        every {
            subsumsjonFetcher.getSubsumsjon("htto://localhost/1234")
        } returns Subsumsjon(
            behovId = "1234",
            faktum = Faktum(
                aktorId = "123",
                vedtakId = -1337,
                beregningsdato = LocalDate.now()
            ),
            grunnlagResultat = null,
            satsResultat = SatsResultat(
                subsumsjonsId = "12",
                sporingsId = "",
                dagsats = 12,
                ukesats = 123,
                regelIdentifikator = "",
                benyttet90ProsentRegel = false
            ),
            minsteinntektResultat = MinsteinntektResultat(
                subsumsjonsId = "12",
                sporingsId = "",
                oppfyllerMinsteinntekt = true,
                minsteinntektInntektsPerioder = emptyList(),
                regelIdentifikator = ""
            ),

            periodeResultat = PeriodeResultat(
                subsumsjonsId = "12",
                sporingsId = "",
                periodeAntallUker = 52,
                regelIdentifikator = ""
            ),
            problem = null
        )

        withTestApplication({
            KalkulatorApp(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Get, "/arbeid/dagpenger/kalkulator-api/behov") {
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
                val behovResponse = moshiInstance.adapter(Map::class.java).fromJson(response.content!!)!!
                behovResponse["oppfyllerMinsteinntekt"] shouldBe true
                behovResponse["ukesats"] shouldBe 123.0
                behovResponse["periodeAntallUker"] shouldBe 52.0
            }
        }
    }

    @Test
    fun `Api returns a 401 if user is unauthenticated`() {
        withTestApplication({
            KalkulatorApp(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Get, "/arbeid/dagpenger/kalkulator-api/behov") {
                addHeader(HttpHeaders.Authorization, "Bearer $unauthorizedToken")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `Apiet burde har metrics endepunkt`() {
        withTestApplication({
            KalkulatorApp(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Get, "/metrics") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
