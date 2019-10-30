package no.nav.dagpenger

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

// Todo: Fjerne konfigurasjonsnøkler som ikke brukes
// todo: rename oidc user and password to applicationuser and password
private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8099",
        "jwks.url" to "https://localhost",
        "jwks.issuer" to "https://localhost",
        "API_GATEWAY_API_KEY" to "hunter2",
        "API_GATEWAY_URL" to "http://localhost/",
        "oidcStsUrl" to "http://localhost/",
            "STS_PASSWORD" to "hai hai",
            "STS_USERNAME" to "kalkulator-api"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "jwks.url" to "https://login.microsoftonline.com/navtestb2c.onmicrosoft.com/discovery/v2.0/keys?p=b2c_1a_idporten",
        "jwks.issuer" to "https://login.microsoftonline.com/d38f25aa-eab8-4c50-9f28-ebf92c1256f2/v2.0/",
        "application.profile" to "DEV",
        "application.httpPort" to "8099",
        "API_GATEWAY_URL" to "https://api-gw-q1.oera.no/dp-reverse-proxy/",
        "oidcStsUrl" to "https://security-token-service.nais.preprod.local"
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "jwks.url" to "https://login.microsoftonline.com/navnob2c.onmicrosoft.com/discovery/v2.0/keys?p=b2c_1a_idporten",
        "jwks.issuer" to "https://login.microsoftonline.com/8b7dfc8b-b52e-4741-bde4-d83ea366f94f/v2.0/",
        "application.profile" to "PROD",
        "application.httpPort" to "8099",
        "API_GATEWAY_URL" to "https://api-gw.oera.no/dp-reverse-proxy/",
        "oidcStsUrl" to "https://security-token-service.nais.adeo.no"
    )
)

// Todo: Fjerne konfigurasjonsnøkler som ikke brukes
data class Configuration(

    val application: Application = Application()

) {
    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)],
        val jwksUrl: String = config()[Key("jwks.url", stringType)],
        val jwksIssuer: String = config()[Key("jwks.issuer", stringType)],
        val name: String = "dp-kalkulator-api",
        val apiGatewayBaseUrl: String = config()[Key("API_GATEWAY_URL", stringType)],
        val apiGatewayKey: String = config()[Key("API_GATEWAY_API_KEY", stringType)],
        val oppslagBaseUrl: String = config()[Key("API_GATEWAY_URL", stringType)] + "dagpenger-oppslag/api",
        val password: String = config()[Key("STS_PASSWORD", stringType)],
        val username: String = config()[Key("STS_USERNAME", stringType)],
        val oicdStsUrl: String = config()[Key("API_GATEWAY_URL", stringType)] + "security-token-service"
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
