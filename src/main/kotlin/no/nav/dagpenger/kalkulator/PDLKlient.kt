package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import no.nav.dagpenger.aad.api.ClientCredentialsClient

internal class PDLKlient(private val oppslagBaseUrl: String, private val tokenProvider: ClientCredentialsClient) {
    suspend fun fetchAktørIdGraphql(fnr: String): String {
        val (_, response, result) = with(oppslagBaseUrl.httpPost()) {
            header("Content-Type" to "application/json")
            header("TEMA", "DAG")
            header("Authorization", "Bearer ${tokenProvider.getAccessToken()}")
            body(MoishiSerDer.adapter.toJson(MoishiSerDer.AktørIdQuery(fnr)))
            responseObject<MoishiSerDer.PDLGraphQlAktørIdResponse>()
        }

        return when (result) {
            is Result.Failure -> throw PDLGraphQlAktørIdException(
                statusCode = response.statusCode,
                message = "Failed to fetch aktoer-id for naturlig-id. Response message $response",
                cause = result.getException()
            )
            is Result.Success -> {
                val aktorIdResponse = result.get()
                if (aktorIdResponse.errors == null) {
                    aktorIdResponse.aktorId()
                } else {
                    throw PDLGraphQlAktørIdException(
                        statusCode = 200,
                        message = "Failed to fetch aktoer-id for naturlig-id. Errors:  ${aktorIdResponse.errors}",
                        cause = null
                    )
                }
            }
        }
    }
}

internal data class PDLGraphQlAktørIdException(
    val statusCode: Int,
    override val message: String,
    override val cause: Throwable?
) : RuntimeException(message, cause)

internal object MoishiSerDer {
    private val moshiInstace: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter: JsonAdapter<GraphQlQuery> = moshiInstace.adapter(GraphQlQuery::class.java)

    open class GraphQlQuery(val query: String, val variables: Any?)
    data class AktørIdQuery(val fnr: String) : GraphQlQuery(
        """query(${'$'}ident: ID!) {
    hentIdenter(ident: ${'$'}ident, grupper: [AKTORID]) {
        identer {
            ident,
            gruppe
        }
    }                
}
    """,
        variables = mapOf("ident" to fnr)
    )

    internal data class PDLGraphQlAktørIdResponse(val data: Data, val errors: List<String>?) {
        data class Data(val hentIdenter: HentIdent?) {
            data class HentIdent(val identer: List<Ident>) {
                data class Ident(val ident: String, val gruppe: String)
            }
        }

        fun aktorId() = data.hentIdenter!!.identer.first { it.gruppe == "AKTORID" }.ident
    }
}
