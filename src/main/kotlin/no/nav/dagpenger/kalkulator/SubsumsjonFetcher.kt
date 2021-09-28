package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import com.github.kittinunf.result.Result
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.aad.api.ClientCredentialsClient

class SubsumsjonFetcher(
    private val regelApiUrl: String,
    private val tokenProvider: ClientCredentialsClient,
) {

    suspend fun getSubsumsjon(subsumsjonLocation: String): Subsumsjon {
        val url = "$regelApiUrl$subsumsjonLocation"

        val jsonAdapter = moshiInstance.adapter(Subsumsjon::class.java)

        val (_, response, result) =
            with(
                url
                    .httpGet()
                    .header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            ) { responseObject(moshiDeserializerOf(jsonAdapter)) }

        return when (result) {
            is Result.Failure -> throw SubsumsjonClientException(
                "Failed to fetch subsumsjon. Response message: ${response.responseMessage}. Error message: ${result.error.message}"
            )
            is Result.Success -> result.get()
        }
    }
}

class SubsumsjonClientException(override val message: String) : RuntimeException(message)
