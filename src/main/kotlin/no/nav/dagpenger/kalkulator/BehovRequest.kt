package no.nav.dagpenger.kalkulator

import java.time.LocalDate
import com.squareup.moshi.Json

//vedtakId er fortsatt required frem til alle er over på regelkontekst
data class BehovRequest(
    val aktorId: String,
    val regelkontekst: RegelKontekst,
    val beregningsdato: LocalDate,
    val vedtakId: Int,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val bruktInntektsPeriode: LocalDate? = null,
    val manueltGrunnlag: Int? = null,
    val antallBarn: Int? = null,
    val inntektsId: String? = null
)

data class RegelKontekst(val id: String, val type: Kontekst)

enum class Kontekst {
    @Json(name = "soknad")
    SOKNAD,
    @Json(name = "veiledning")
    VEILEDNING,
    @Json(name = "vedtak")
    VEDTAK,
}