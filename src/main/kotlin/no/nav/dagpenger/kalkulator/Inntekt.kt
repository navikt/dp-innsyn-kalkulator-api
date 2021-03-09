package no.nav.dagpenger.kalkulator

import java.math.BigDecimal

data class Inntekt(
    val inntekt: BigDecimal,
    val periode: Int,
    val inntektsPeriode: InntektsPeriode,
    val inneholderFangstOgFisk: Boolean,
    val andel: BigDecimal? = null
) {
    init {
        val gyldigePerioder = setOf(1, 2, 3)
        if (!gyldigePerioder.contains(periode)) {
            throw IllegalArgumentException("Ugyldig periode for inntektgrunnlat, gyldige verdier er ${gyldigePerioder.joinToString { "$it" }}")
        }
    }
}
