package no.nav.dagpenger.kalkulator

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.naischecks() {
    get("/isAlive") {
        call.respondText("ALIVE", ContentType.Text.Plain)
    }
    get("/isReady") {
        call.respondText("READY", ContentType.Text.Plain)
    }
}
