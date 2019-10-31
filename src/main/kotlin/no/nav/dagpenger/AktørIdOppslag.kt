package no.nav.dagpenger
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.moshi.responseObject
import com.github.kittinunf.result.Result
import mu.KotlinLogging
import java.lang.RuntimeException

private val logger = KotlinLogging.logger {}
private val adapter = moshiInstance.adapter(GraphQlQuery::class.java).serializeNulls()

class AktørIdOppslag(private val oppslagBaseUrl: String, private val apiGatewayKey: String) {

    fun fetchAktørIdGraphql(fnr: String, idToken: String): Bruker? {
        val (_, response, result) = with(oppslagBaseUrl.httpGet()) {
            header("Content-Type" to "application/json")
            header("x-nav-apiKey" to apiGatewayKey)
            header("ID_token" to idToken)
            body(adapter.toJson(aktørIdQuery(fnr)))
            responseObject<GraphQlAktørIdResponse>()
        }

        return when (result) {
            is Result.Failure -> throw GraphQlAktørIdException(
                    response.statusCode,
                    "Failed to fetch aktoer-id for naturlig-id. Response message ${response.responseMessage}",
                    result.getException()
            )
            is Result.Success -> result.get().data.bruker
        }
    }

    // todo: remove everything related to dummy route
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
}

// todo: remove everything related to dummy route
data class BOB(val orgNr: String, val navn: String)

sealed class GraphQlQuery(val query: String, val variables: Any?)

data class aktørIdQuery(val fnr: String) : GraphQlQuery(
        query = """ 
            query PersonFromFnrQuery($fnr: ID!) {
    person(id: $fnr, idType: NATURLIG_IDENT) {
        aktoerId
    }
}
            """.trimIndent(),
        variables = null
)

data class Data(val bruker: Bruker)

data class Bruker(
    val type: BrukerType,
    val id: String
)

enum class BrukerType {
    ORGNR, AKTOERID, FNR
}

data class GraphQlAktørIdResponse(val data: Data, val errors: List<String>?)

class GraphQlAktørIdException(val statusCode: Int, override val message: String, override val cause: Throwable) :
        RuntimeException(message, cause)