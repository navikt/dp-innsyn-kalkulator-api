package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import io.prometheus.client.Counter
import java.time.LocalDate

private val kontekstCounter = Counter
    .build()
    .name("kalkulator_kontekst")
    .help("Hvilken kontekst kalkulatoren blir brukt i")
    .labelNames("kontekst")
    .register()

class BehovStarter(
    private val regelApiUrl: String,
    private val regelApiKey: String,
    private val apiGatewayKey: String
) {
    private val jsonAdapter = moshiInstance.adapter(BehovRequest::class.java)

    fun startBehov(aktørId: String, regelkontekst: String): String {
        kontekstCounter.labels(regelkontekst).inc()

        val behovUrl = "$regelApiUrl/behov"
        val json = jsonAdapter.toJson(createBehovRequest(aktørId, regelkontekst))

        val (_, response, result) =
            behovUrl.httpPost()
                .apiKey(regelApiKey)
                .header("x-nav-apiKey" to apiGatewayKey)
                .header(mapOf("Content-Type" to "application/json"))
                .body(json)
                .response()

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

private fun createBehovRequest(aktørId: String, regelkontekstType: String): BehovRequest {
    val vedtakId = -1337
    return BehovRequest(
        aktørId,
        vedtakId,
        LocalDate.now(),
        regelkontekst = RegelKontekst(type = regelkontekstType, id = vedtakId.toString())
    ) // TODO: Når regel-api håndterer at vi kan fjerne vedtakid må den vekk herfra
}
