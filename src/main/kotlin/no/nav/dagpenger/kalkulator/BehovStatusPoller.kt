package no.nav.dagpenger.kalkulator

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.moshi.responseObject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.time.Duration

private val LOGGER = KotlinLogging.logger {}

class BehovStatusPoller(
        private val regelApiUrl: String,
        private val regelApiKey: String,
        private val apiGatewayKey: String,
        private val timeout: Duration = Duration.ofSeconds(20)
) {
    private val delayDuration = Duration.ofMillis(100)

    private fun pollInternal(statusUrl: String): BehovStatusPollResult {

        val (_, response, result) =
                with(
                        statusUrl
                                .httpGet()
                                .header("x-nav-apiKey" to apiGatewayKey)
                                .apiKey(regelApiKey)
                                .allowRedirects(false)
                ) { responseObject<BehovStatusResponse>() }

        return try {
            BehovStatusPollResult(result.get().status, null)
        } catch (exception: Exception) {
            if (response.statusCode == 303) {
                LOGGER.info("Caught 303: $response")
                return BehovStatusPollResult(
                        null,
                        response.headers["Location"].first()
                )
            } else {
                throw PollSubsumsjonStatusException(
                        response.responseMessage, exception
                )
            }
        }
    }

    suspend fun pollStatus(statusUrl: String): String {
        val url = "$regelApiUrl$statusUrl"
        try {
            return withTimeout(timeout.toMillis()) {
                return@withTimeout pollWithDelay(url)
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
        val status: BehovStatus?,
        val location: String?
) {
    fun isPending() = status == BehovStatus.PENDING
}

class PollSubsumsjonStatusException(
        override val message: String,
        override val cause: Throwable? = null
) : RuntimeException(message, cause)

class RegelApiTimeoutException(override val message: String) : RuntimeException(message)
