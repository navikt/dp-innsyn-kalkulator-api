package no.nav.dagpenger.kalkulator

import java.time.LocalDate

data class BehovRequest(
    val aktorId: String,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val bruktInntektsPeriode: LocalDate? = null,
    val manueltGrunnlag: Int? = null,
    val antallBarn: Int? = null,
    val regelkontekst: RegelKontekst,
    val inntektsId: String? = null
)

data class RegelKontekst(
    val type: String,
)
