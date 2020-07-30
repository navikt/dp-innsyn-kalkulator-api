package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import com.github.kittinunf.result.Result

class SubsumsjonFetcher(
    private val regelApiUrl: String,
    private val regelApiKey: String,
    private val apiGatewayKey: String
) {

    fun getSubsumsjon(subsumsjonLocation: String): Subsumsjon {
        val url = "$regelApiUrl$subsumsjonLocation"

        val jsonAdapter = moshiInstance.adapter(Subsumsjon::class.java)

        val (_, response, result) =
            with(
                url
                    .httpGet()
                    .header("x-nav-apiKey" to apiGatewayKey)
                    .apiKey(regelApiKey)
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
