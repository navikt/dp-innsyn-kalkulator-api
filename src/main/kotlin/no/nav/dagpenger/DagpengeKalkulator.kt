package no.nav.dagpenger

import no.nav.dagpenger.regel.api.internal.BehovStatusPoller
import no.nav.dagpenger.regel.api.internal.SubsumsjonFetcher

class DagpengeKalkulator(
    val behovStarter: BehovStarter,
    val behovStatusPoller: BehovStatusPoller,
    val subsumsjonFetcher: SubsumsjonFetcher
) {
    suspend fun kalkuler(aktørId: String): KalkulasjonsResult {
        // LOGGER.info { "starting behov, trying " + config.application.regelApiBaseUrl + "/behov" }
        val pollLocation = behovStarter.startBehov(aktørId)

        val subsumsjonLocation = behovStatusPoller.pollStatus(pollLocation)

        val subsumsjon = subsumsjonFetcher.getSubsumsjon(subsumsjonLocation)

        return KalkulasjonsResult(
                subsumsjon.minsteinntektResultat?.oppfyllerMinsteinntekt ?: throw IncompleteResultException("Missing minsteinntektResultat"),
                subsumsjon.satsResultat?.ukesats ?: throw IncompleteResultException("Missing satsResultat"),
                subsumsjon.periodeResultat?.periodeAntallUker ?: throw IncompleteResultException("Missing periodeResultat")
        )
    }
}

class IncompleteResultException(override val message: String) : RuntimeException(message)

data class KalkulasjonsResult(
    val oppfyllerMinsteinntekt: Boolean,
    val ukesats: Int,
    val periodeAntallUker: Int
)