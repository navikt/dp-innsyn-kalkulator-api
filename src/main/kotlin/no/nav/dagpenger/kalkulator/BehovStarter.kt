package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import io.ktor.http.HttpHeaders
import io.prometheus.client.Counter
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import java.time.LocalDate

private val kontekstCounter = Counter
    .build()
    .name("kalkulator_kontekst")
    .help("Hvilken kontekst kalkulatoren blir brukt i")
    .labelNames("kontekst")
    .register()

internal class BehovStarter(
    private val regelApiUrl: String,
    private val tokenProvider: ClientCredentialsClient,
) {
    private val jsonAdapter = moshiInstance.adapter(BehovRequest::class.java)

    suspend fun startBehov(aktørId: String, regelkontekst: String): String {
        kontekstCounter.labels(regelkontekst).inc()

        val behovUrl = "$regelApiUrl/behov"
        val json = jsonAdapter.toJson(createBehovRequest(aktørId, regelkontekst))

        val (_, response, result) =
            behovUrl.httpPost()
                .header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
                .header(mapOf("Content-Type" to "application/json"))
                .body(json)
                .response()

        return when (result) {
            is Result.Failure -> throw RegelApiBehovHttpClientException(
                "Failed to run behov. Response message ${response.responseMessage}. Error message: ${result.error.message}"
            )
            is Result.Success ->
                response.headers["Location"].first().replaceFirst("/v1/", "/")
        }
    }
}

class RegelApiBehovHttpClientException(override val message: String) : RuntimeException(message)

private fun createBehovRequest(aktørId: String, regelkontekstType: String): BehovRequest {
    return BehovRequest(
        aktørId,
        LocalDate.now(),
        regelkontekst = RegelKontekst(type = regelkontekstType)
    )
}
