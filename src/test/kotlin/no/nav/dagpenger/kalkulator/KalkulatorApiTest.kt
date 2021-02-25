package no.nav.dagpenger.kalkulator

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KalkulatorApiTest {
    private val jwkStub = JwtStub()
    private val token = jwkStub.createTokenFor("brukerMcBrukerson")
    private val unauthorizedToken = "tull"

    private val aktørIdOppslagKlient: AktørIdOppslagKlient = mockk()
    private val behovStarter: BehovStarter = mockk()
    private val behovStatusPoller: BehovStatusPoller = mockk()
    private val subsumsjonFetcher: SubsumsjonFetcher = mockk()
    private val dagpengeKalkulator = DagpengeKalkulator(behovStarter, behovStatusPoller, subsumsjonFetcher)

    @Test
    fun `Startbehov returns a response`() {
        withTestApplication({
            KalkulatorApi(
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

            handleRequest(HttpMethod.Get, "/arbeid/dagpenger/kalkulator-api/behov?kontekst=veiledning") {
            }.apply {
                assertNotNull(response.status())
            }
        }
    }

    @Test
    fun `Startbehov returns a response with real token`() {
        withTestApplication({
            KalkulatorApi(
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
    fun `Skal kunne kalkulere dagpenge sats og periode, samt inngangsvilkår minste arbeidsinntekt og grunnlag avkortet`() {
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
            grunnlagResultat = GrunnlagResultat(
                subsumsjonsId = "grunnlagSubsumsjonId",
                sporingsId = "",
                regelIdentifikator = "",
                avkortet = BigDecimal(20.5),
                uavkortet = BigDecimal(10),
                harAvkortet = true,
                beregningsregel = "",
                grunnlagInntektsPerioder = listOf()
            ),
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
            KalkulatorApi(
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
                behovResponse["avkortetGrunnlag"] shouldBe "20.5"
                behovResponse["subsumsjonId"] shouldBe "grunnlagSubsumsjonId"
            }
        }
    }

    @Test
    fun `videresender regelkontekst fra query-parameter`() {
        val regelkontekst = "veiledning"

        every {
            aktørIdOppslagKlient.fetchAktørIdGraphql(any(), any())
        } returns Person("1234")

        every {
            behovStarter.startBehov("1234", regelkontekst)
        } returns "htto://localhost/1234"

        every {
            behovStarter.startBehov("")
        } returns "htto://localhost/1234"

        withTestApplication({
            KalkulatorApi(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Get, "/arbeid/dagpenger/kalkulator-api/behov?regelkontekst=$regelkontekst") {
                addHeader(HttpHeaders.Cookie, "selvbetjening-idtoken=$token")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody(
                    """
                        {
                            "beregningsdato": "2019-06-05"
                        }
                    """.trimIndent()
                )
            }
        }
        verify { behovStarter.startBehov("1234", regelkontekst) }
    }

    @Test
    fun `Api returns a 401 if user is unauthenticated`() {
        withTestApplication({
            KalkulatorApi(
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
    fun `Returnerer 504 hvis Subsumsjonen ikke er komplett`() {
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
            minsteinntektResultat = null,

            periodeResultat = PeriodeResultat(
                subsumsjonsId = "12",
                sporingsId = "",
                periodeAntallUker = 52,
                regelIdentifikator = ""
            ),
            problem = null
        )

        withTestApplication({
            KalkulatorApi(
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
                assertEquals(HttpStatusCode.GatewayTimeout, response.status())
            }
        }
    }

    @Test
    fun ` Skal ikke kunne reberegne behov uten api nøkkel `() {

        withTestApplication({
            KalkulatorApi(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Post, "/arbeid/dagpenger/kalkulator-api/behov/reberegning") {
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun ` Skal ikke kunne reberegne behov uten fødselsnummer `() {

        withTestApplication({
            KalkulatorApi(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Post, "/arbeid/dagpenger/kalkulator-api/behov/reberegning") {
                addHeader("x-api-key", "hunter2")
                addHeader("Content-Type", "application/json")
                setBody("{}")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun ` Skal kunne reberegne behov når alle parametre er satt `() {

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
            grunnlagResultat = GrunnlagResultat(
                subsumsjonsId = "grunnlagSubsumsjonId",
                sporingsId = "",
                regelIdentifikator = "",
                avkortet = BigDecimal(20.5),
                uavkortet = BigDecimal(10),
                harAvkortet = true,
                beregningsregel = "",
                grunnlagInntektsPerioder = listOf()
            ),
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
            KalkulatorApi(
                jwkStub.stubbedJwkProvider(),
                "test issuer",
                aktørIdOppslagKlient,
                dagpengeKalkulator
            )
        }) {
            handleRequest(HttpMethod.Post, "/arbeid/dagpenger/kalkulator-api/behov/reberegning") {
                addHeader("x-api-key", "hunter2")
                addHeader("Content-Type", "application/json")
                setBody("""{ "fnr": "123423" }""")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun `Apiet burde har metrics endepunkt`() {
        withTestApplication({
            KalkulatorApi(
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
