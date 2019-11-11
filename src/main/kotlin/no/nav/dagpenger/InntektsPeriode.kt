package no.nav.dagpenger

import java.time.YearMonth

data class InntektsPeriode(
    val førsteMåned: YearMonth,
    val sisteMåned: YearMonth
)