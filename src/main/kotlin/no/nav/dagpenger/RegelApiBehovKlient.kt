package no.nav.dagpenger
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

class RegelApiBehovKlient(private val regelApiUrl: String, private val regelApiKey: String) {
    private val jsonAdapter = moshiInstance.adapter(BehovRequest::class.java)

    fun StartBehov(behovRequest: BehovRequest): String {
        val behovUrl = "$regelApiUrl/behov"
        // val json = jsonAdapter.toJson(behovRequest)

        val (_, response, result) =

                        behovUrl.httpPost()
                                .apiKey(regelApiKey)
                                // .header(mapOf("Content-Type" to "application/json"))
                                // .body(json)
                                .response()

        return when (result) {
            is Result.Failure -> throw RegelApiBehovHttpClientException(
                    "Failed to run behov. Response message ${response.responseMessage}. Error message: ${result.error.message}")
            is Result.Success ->
                response.headers["Location"].first()
        }
    }
}

class RegelApiBehovHttpClientException(override val message: String) : RuntimeException(message)