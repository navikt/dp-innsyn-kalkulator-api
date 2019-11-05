package no.nav.dagpenger

enum class BehovStatus {
    PENDING
}

data class BehovStatusResponse(val status: BehovStatus)