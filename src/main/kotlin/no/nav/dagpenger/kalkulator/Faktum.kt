package no.nav.dagpenger.kalkulator

import java.time.LocalDate

data class Faktum(
        val aktorId: String,
        val vedtakId: Int,
        val beregningsdato: LocalDate,
        val inntektsId: String? = null,
        val inntektAvvik: Boolean? = null,
        val inntektManueltRedigert: Boolean? = null,
        val harAvtjentVerneplikt: Boolean? = null,
        val oppfyllerKravTilFangstOgFisk: Boolean? = null,
        val antallBarn: Int? = null,
        val manueltGrunnlag: Int? = null,
        val bruktInntektsPeriode: InntektsPeriode? = null
)
