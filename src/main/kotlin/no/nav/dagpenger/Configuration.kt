package no.nav.dagpenger

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8099",
        "jwks.url" to "https://localhost",
        "jwks.issuer" to "https://localhost",
            "API_GATEWAY_API_KEY" to "superhemmelig",
            "oppslagBaseUrl" to "www.nrk.no",
            "oidcPassword" to "hunter2",
            "oidcUsername" to "Fantomet",
            "oidcStsUrl" to "www.oidc.no"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "jwks.url" to "https://isso-q.adeo.no:443/isso/oauth2/connect/jwk_uri",
        "jwks.issuer" to "https://isso-q.adeo.no:443/isso/oauth2",
        "application.profile" to "DEV",
        "application.httpPort" to "8099",
            "API_GATEWAY_API_KEY" to "hunter2",
            "oppslagBaseUrl" to "www.nrk.no",
            "oidcPassword" to "hunter2",
            "oidcUsername" to "Fantomet",
            "oidcStsUrl" to "www.oidc.no"
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "jwks.url" to "https://isso.adeo.no:443/isso/oauth2/connect/jwk_uri",
        "jwks.issuer" to "https://isso.adeo.no:443/isso/oauth2",
        "application.profile" to "PROD",
        "application.httpPort" to "8099",
            "API_GATEWAY_API_KEY" to "hunter2",
            "oppslagBaseUrl" to "www.nrk.no",
            "oidcPassword" to "hunter2",
            "oidcUsername" to "Fantomet",
            "oidcStsUrl" to "www.oidc.no"
    )
)

data class Configuration(

    val application: Application = Application()

) {
    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)],
        val jwksUrl: String = config()[Key("jwks.url", stringType)],
        val jwksIssuer: String = config()[Key("jwks.issuer", stringType)],
        val name: String = "dp-kalkulator-api",
        val gatewayKey: String = config()[Key("API_GATEWAY_API_KEY", stringType)],
        val oppslagBaseUrl: String = config()[Key("oppslagBaseUrl", stringType)],
        val password: String = config()[Key("oidcPassword", stringType)],
        val username: String = config()[Key("oidcUsername", stringType)],
        val oicdStsUrl: String = config()[Key("oidcStsUrl", stringType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}

fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-sbs" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-sbs" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}
