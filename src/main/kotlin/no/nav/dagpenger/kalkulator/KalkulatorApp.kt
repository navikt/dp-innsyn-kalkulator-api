package no.nav.dagpenger.kalkulator

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.exceptions.JWTDecodeException
import com.ryanharter.ktor.moshi.moshi
import com.squareup.moshi.JsonDataException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.internal.BehovStatusPoller
import no.nav.dagpenger.regel.api.internal.SubsumsjonFetcher
import org.slf4j.event.Level
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}
val config = Configuration()

fun main() = runBlocking {
    val jwkProvider = JwkProviderBuilder(URL(config.application.jwksUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    val aktørIdOppslag = AktørIdOppslagKlient(config.application.graphQlBaseUrl, config.application.apiGatewayKey, config.application.graphQlKey)
    val behovStarter = BehovStarter(config.application.regelApiBaseUrl, config.auth.regelApiKey, config.application.apiGatewayKey)
    val behovStatusPoller = BehovStatusPoller(config.application.regelApiBaseUrl, config.auth.regelApiKey, config.application.apiGatewayKey)
    val subsumsjonFetcher = SubsumsjonFetcher(config.application.regelApiBaseUrl, config.auth.regelApiKey, config.application.apiGatewayKey)
    val dagpengeKalkulator = DagpengeKalkulator(behovStarter, behovStatusPoller, subsumsjonFetcher)

    val application = embeddedServer(Netty, port = config.application.httpPort) {
        KalkulatorApi(jwkProvider, config.application.jwksIssuer, aktørIdOppslag, dagpengeKalkulator)
        LOGGER.debug("Starting application")
    }.also {
        it.start(wait = false)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        application.stop(10, 60, TimeUnit.SECONDS)
    })
}

fun Application.KalkulatorApi(
    jwkProvider: JwkProvider,
    jwtIssuer: String,
    aktørIdKlient: AktørIdOppslagKlient,
    dagpengerKalkulator: DagpengeKalkulator
) {
    install(ContentNegotiation) {
        moshi(moshiInstance)
    }
    install(CallLogging) {
        level = Level.INFO

        filter { call ->
            !call.request.path().startsWith("/isAlive") &&
                    !call.request.path().startsWith("/isReady") &&
                    !call.request.path().startsWith("/metrics")
        }
    }

    install(MicrometerMetrics) {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)
    }

    install(Authentication) {
        jwt {
            verifier(jwkProvider, jwtIssuer)
            realm = "dp-kalkulator-api"
            authHeader {
                val cookie = it.request.cookies["selvbetjening-idtoken"]
                        ?: throw CookieNotSetException("Cookie with name selvbetjening-idtoken not found")
                HttpAuthHeader.Single("Bearer", cookie)
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            LOGGER.info("Generic exception! - ¯\\_(ツ)_/¯", cause)
            LOGGER.error("Unhåndtert feil ved beregning av regel", cause)
            val problem = Problem(
                    title = "Uhåndtert feil",
                    detail = cause.message
            )
            call.respond(HttpStatusCode.InternalServerError, problem)
        }
        exception<JsonDataException> { cause ->
            LOGGER.info("JsonDataException! - RegelApi godtok ikke parametrene våre", cause)
            LOGGER.warn(cause.message, cause)
            val status = HttpStatusCode.BadRequest
            val problem = Problem(
                    type = URI.create("urn:dp:error:parameter"),
                    title = "Parameteret er ikke gyldig, mangler obligatorisk felt: '${cause.message}'",
                    status = status.value
            )
            call.respond(status, problem)
        }
        exception<CookieNotSetException> { cause ->
            LOGGER.info("CookieNotSetException! (ikke innlogget)", cause)
            val status = HttpStatusCode.Unauthorized
            val problem = Problem(
                    title = "Ikke innlogget",
                    detail = "${cause.message}",
                    status = status.value
            )
            call.respond(status, problem)
        }
        // todo: fix this errorhandling?
        exception<RegelApiBehovHttpClientException> { cause ->
            LOGGER.info("RegelApiBehovClientException! - Har ikke fått subsumsjonslokalsjon tross startet behov", cause)
            LOGGER.warn("Couldn't get Subsumsjon: " + cause.message, cause)
            val status = HttpStatusCode.BadGateway
            val problem = Problem(
                    title = "Feil fra regel-api",
                    detail = cause.message,
                    status = status.value
            )
            call.respond(status, problem)
        }
        exception<IncompleteResultException> { cause ->
            LOGGER.info("IncompleteResultException! - Ikke kontakt med skatt?", cause)
            LOGGER.warn("IncompleteResultException: " + cause.message, cause)
            val status = HttpStatusCode.GatewayTimeout
            val problem = Problem(
                    title = "Feil fra API, fikk ikke beregnet inntekt",
                    detail = cause.message,
                    status = status.value
            )
            call.respond(status, problem)
        }
    }
    routing {
        authenticate {
            route("/arbeid/dagpenger/kalkulator-api/behov") {
                get {
                    val idToken = call.request.cookies["selvbetjening-idtoken"]
                            ?: throw CookieNotSetException("Cookie with name selvbetjening-idtoken not found")
                    val fødselsnummer = getSubject()
                    LOGGER.info { "fetching aktør from " + config.application.graphQlBaseUrl }
                    LOGGER.info { "graphql keylength: " + config.application.graphQlKey.length }
                    val person = aktørIdKlient.fetchAktørIdGraphql(fødselsnummer, idToken)

                    val response = dagpengerKalkulator.kalkuler(person.aktoerId)

                    call.respond(HttpStatusCode.OK, response)
                }
            }
            route("/arbeid/dagpenger/kalkulator-api/auth") {
                get {
                    call.respond(HttpStatusCode.OK, "Gyldig token!")
                }
            }
        }
        naischecks()
        metrics()
    }
}

private fun PipelineContext<Unit, ApplicationCall>.getSubject(): String {
    return runCatching {
        call.authentication.principal?.let {
            (it as JWTPrincipal).payload.subject
        } ?: throw JWTDecodeException("Unable to get subject from JWT")
    }.getOrElse {
        LOGGER.error(it) { "Unable to get subject from authentication" }
        return@getOrElse "UNKNOWN"
    }
}

class CookieNotSetException(override val message: String?) : RuntimeException(message)

data class BehovResponse(val location: String)
