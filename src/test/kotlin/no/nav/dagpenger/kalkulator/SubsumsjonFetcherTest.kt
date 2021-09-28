package no.nav.dagpenger.kalkulator

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubsumsjonFetcherTest {

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
    fun `Should get subsumsjon`() {

        val responseBodyJson = SubsumsjonFetcherTest::class.java
            .getResource("/example-subsumsjon-payload.json").readText()

        val equalToPattern = EqualToPattern("Bearer testToken")
        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("//subsumsjon/112233"))
                .withHeader("Authorization", equalToPattern)
                .willReturn(
                    WireMock.aResponse()
                        .withBody(responseBodyJson)
                )
        )

        val client = SubsumsjonFetcher(server.url(""), tokenProviderMock)

        val subsumsjon = runBlocking { client.getSubsumsjon("/subsumsjon/112233") }

        assertEquals("01DBFXH3N1BSSVXB6X0GVFV7X3", subsumsjon.minsteinntektResultat!!.subsumsjonsId)
        assertEquals("01DBFXH3RHDGZAD0A7ZZYNDTKW", subsumsjon.periodeResultat!!.subsumsjonsId)
        assertEquals("01DBFXH3PBT5MSBEB9RN3WQRMS", subsumsjon.grunnlagResultat!!.subsumsjonsId)
        assertEquals("01DBFXH3VP2FKZG331B0S9VE04", subsumsjon.satsResultat!!.subsumsjonsId)
    }
}
