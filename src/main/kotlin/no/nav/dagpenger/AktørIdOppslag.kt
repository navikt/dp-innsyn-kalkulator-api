package no.nav.dagpenger
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.moshi.responseObject
import mu.KotlinLogging
import no.nav.dagpenger.oidc.OidcClient
import no.nav.dagpenger.oidc.OidcToken
import java.lang.RuntimeException

private val logger = KotlinLogging.logger {}

class AktørIdOppslag(private val oppslagBaseUrl: String, val oidcClient: OidcClient, val apiGatewayKey: String) {

    fun fetchAktørId(fnr: String): String? {
        return withOidc { token ->
            val url = "$oppslagBaseUrl/aktoer-ident"
            val (_, _, result) = with(
                    url.httpGet()
                            .authentication().bearer(token.access_token)
                            .header(
                                    mapOf(
                                            "ident" to fnr,
                                            "x-nav-apiKey" to apiGatewayKey
                                    )
                            )
            ) {
                responseObject<AktørIdResponse>()
            }
            result.fold({ success ->
                success.aktørId
            }, { error ->
                logger.warn(
                        "Feil ved oppslag av personnummer",
                        OppslagException(error.response.statusCode, error.message ?: "")
                )
                null
            })
        }
    }

    fun fetchOrganisasjonsNavn(): Any {
        val url = "$oppslagBaseUrl/organisasjon/123456789"

        val (_, _, result) = with(
                url.httpGet()
                        .header(
                                mapOf(
                                        "x-nav-apiKey" to apiGatewayKey
                                )
                        )
        ) {
            responseObject<BOB>()
        }

        return result.get()
    }

    private fun <T> withOidc(function: (value: OidcToken) -> T?): T? =
            runCatching { oidcClient.oidcToken() }.fold(function, onFailure = {
                logger.warn("Feil ved henting av OIDC token", OppslagException(500, it.message ?: ""))
                null
            })
}

data class BOB(val orgNr: String, val navn: String)

data class AktørIdResponse(val aktørId: String)

class OppslagException(val statusCode: Int, override val message: String) :
        RuntimeException(message)