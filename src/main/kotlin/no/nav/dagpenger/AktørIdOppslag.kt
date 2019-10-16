package no.nav.dagpenger
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.responseObject
import io.ktor.http.HttpHeaders
import mu.KotlinLogging
import no.nav.dagpenger.oidc.OidcClient
import no.nav.dagpenger.oidc.OidcToken
import java.lang.RuntimeException

private val logger = KotlinLogging.logger {}

class AktørIdOppslag(private val oppslagBaseUrl: String, private val apiKey: String, val oidcClient: OidcClient) {

    fun fetchAktørId(fnr: String): String? {
        return withOidc { token ->
            val url = "$oppslagBaseUrl/aktoer-ident"
            val (_, _, result) = with(
                    url.httpGet()
                            .authentication().bearer(token.access_token)
                            .header(
                                    mapOf(
                                            "ident" to fnr
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
    private fun <T> withOidc(function: (value: OidcToken) -> T?): T? =
            runCatching { oidcClient.oidcToken() }.fold(function, onFailure = {
                logger.warn("Feil ved henting av OIDC token", OppslagException(500, it.message ?: ""))
                null
            })

}

data class AktørIdResponse(val aktørId: String)

class OppslagException(val statusCode: Int, override val message: String) :
        RuntimeException(message)