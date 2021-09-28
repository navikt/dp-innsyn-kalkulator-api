package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import java.time.Duration

private val LOGGER = KotlinLogging.logger {}

class BehovStatusPoller(
    private val regelApiUrl: String,
    private val tokenProvider: ClientCredentialsClient,
    private val timeout: Duration = Duration.ofSeconds(20)
) {
    private val delayDuration = Duration.ofMillis(100)

    private suspend fun pollInternal(statusUrl: String): BehovStatusPollResult {
        val (_, response, result) = statusUrl.httpGet()
            .header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            .header(mapOf("Content-Type" to "application/json"))
            .allowRedirects(false)
            .responseString()

        LOGGER.info { "Got ${response.statusCode} from $statusUrl" }

        return result.fold(
            success = {
                when (response.statusCode) {
                    303 -> BehovStatusPollResult(
                        pending = false,
                        location = response.headers["Location"].first().replaceFirst("/v1/", "/")
                    )
                    else -> {
                        BehovStatusPollResult(
                            pending = true,
                            location = null
                        )
                    }
                }
            },
            failure = {
                LOGGER.error("Failed polling $statusUrl")
                throw PollSubsumsjonStatusException(
                    response.responseMessage,
                    it.exception
                )
            }
        )
    }

    suspend fun pollStatus(statusUrl: String): String {
        val url = "$regelApiUrl$statusUrl"

        try {
            return withContext(Dispatchers.IO) {
                withTimeout(timeout.toMillis()) {
                    return@withTimeout pollWithDelay(url)
                }
            }
        } catch (e: Exception) {
            when (e) {
                is TimeoutCancellationException -> throw RegelApiTimeoutException("Polled behov status for more than ${timeout.toMillis()} milliseconds")
                else -> throw PollSubsumsjonStatusException("Failed", e)
            }
        }
    }

    private suspend fun pollWithDelay(statusUrl: String): String {
        val status = pollInternal(statusUrl)
        return if (status.isPending()) {
            delay(delayDuration)
            pollWithDelay(statusUrl)
        } else {
            status.location ?: throw PollSubsumsjonStatusException("Did not get location with task")
        }
    }
}

private data class BehovStatusPollResult(
    private val pending: Boolean,
    val location: String?
) {
    fun isPending() = pending
}

class PollSubsumsjonStatusException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class RegelApiTimeoutException(override val message: String) : RuntimeException(message)
