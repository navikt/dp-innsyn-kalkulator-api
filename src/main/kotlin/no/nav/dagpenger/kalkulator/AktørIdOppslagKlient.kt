package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
internal val adapter = moshiInstance.adapter(GraphQlQuery::class.java).serializeNulls()

internal open class GraphQlQuery(val query: String, val variables: Any?)

class AktørIdOppslagKlient(private val oppslagBaseUrl: String, private val apiGatewayKey: String, private val graphQlKey: String) {

    fun fetchAktørIdGraphql(fnr: String, idToken: String? = null): Person {
        val (_, response, result) = with(oppslagBaseUrl.httpPost()) {
            header("Content-Type" to "application/json")
            header("x-nav-apiKey" to apiGatewayKey)
            header("x-api-key" to graphQlKey)
            if (idToken != null) header("ID_token" to idToken)
            body(adapter.toJson(aktørIdQuery(fnr)))
            responseObject<GraphQlAktørIdResponse>()
        }

        return when (result) {
            is Result.Failure -> throw GraphQlAktørIdException(
                response.statusCode,
                "Failed to fetch aktoer-id for naturlig-id. Response message $response",
                result.getException()
            )
            is Result.Success -> result.get().data.person
        }
    }
}

internal data class aktørIdQuery(val fnr: String) : GraphQlQuery(
    query =
        """ 
            query {
                person(id: "$fnr", idType: NATURLIG_IDENT) {
                aktoerId
    }
}
        """.trimIndent(),
    variables = null
)

data class Data(val person: Person)

data class Person(
    val aktoerId: String
)

data class GraphQlAktørIdResponse(val data: Data, val errors: List<String>?)

class GraphQlAktørIdException(val statusCode: Int, override val message: String, override val cause: Throwable) :
    RuntimeException(message, cause)
