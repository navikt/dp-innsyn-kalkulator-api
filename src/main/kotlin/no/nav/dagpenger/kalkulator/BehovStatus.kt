package no.nav.dagpenger.kalkulator

enum class BehovStatus {
    PENDING
}

data class BehovStatusResponse(val status: BehovStatus)
