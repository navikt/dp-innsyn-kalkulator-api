package no.nav.dagpenger
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.responseObject
import com.github.kittinunf.result.Result
import java.time.LocalDate
import no.nav.dagpenger.BehovStatusResponse

class RegelApiBehovKlient(private val regelApiUrl: String, private val regelApiKey: String) {
    private val jsonAdapter = moshiInstance.adapter(BehovRequest::class.java)

    fun run(behovRequest: BehovRequest): String {
        val behovUrl = "$regelApiUrl/behov"

        val json = jsonAdapter.toJson(behovRequest)

        val (_, response, result) =
                with(
                        behovUrl.httpPost()
                                .apiKey(regelApiKey)
                                .header(mapOf("Content-Type" to "application/json"))
                                .body(json)
                ) {
                    responseObject<BehovStatusResponse>()
                }
        return when (result) {
            is Result.Failure -> throw RegelApiBehovHttpClientException(
                    "Failed to run behov. Response message ${response.responseMessage}. Error message: ${result.error.message}")
            is Result.Success ->
                response.headers["Location"].first()
        }
    }
}

data class BehovRequest(
        val aktorId: String,
        val vedtakId: Int,
        val beregningsdato: LocalDate,
        val harAvtjentVerneplikt: Boolean? = null,
        val oppfyllerKravTilFangstOgFisk: Boolean? = null,
        val bruktInntektsPeriode: LocalDate? = null,
        val manueltGrunnlag: Int? = null,
        val antallBarn: Int? = null,
        val inntektsId: String? = null
)

class RegelApiBehovHttpClientException(override val message: String) : RuntimeException(message)