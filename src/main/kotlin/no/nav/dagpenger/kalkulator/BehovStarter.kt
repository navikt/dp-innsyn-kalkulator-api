package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import java.time.LocalDate

class BehovStarter(private val regelApiUrl: String, private val regelApiKey: String, private val apiGatewayKey: String) {
    private val jsonAdapter = moshiInstance.adapter(BehovRequest::class.java)

    fun startBehov(aktørId: String): String {
        val behovUrl = "$regelApiUrl/behov"
        val json = jsonAdapter.toJson(createBehovRequest(aktørId))

        val (_, response, result) =
            with(
                behovUrl.httpPost()
                    .apiKey(regelApiKey)
                    .header("x-nav-apiKey" to apiGatewayKey)
                    .header(mapOf("Content-Type" to "application/json"))
                    .body(json)
            ) {
                responseObject<BehovStatusResponse>()
            }
        return when (result) {
            is Result.Failure -> throw RegelApiBehovHttpClientException(
                "Failed to run behov. Response message ${response.responseMessage}. Error message: ${result.error.message}"
            )
            is Result.Success ->
                response.headers["Location"].first()
        }
    }
}

class RegelApiBehovHttpClientException(override val message: String) : RuntimeException(message)

private fun createBehovRequest(aktørId: String): BehovRequest {
    return BehovRequest(aktørId, -1337, LocalDate.now())
}
