package no.nav.dagpenger.kalkulator

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8099",
        "loginservice.idporten.discovery.url" to "https://localhost",
        "loginservice.idporten.audience" to "https://localhost",
        "API_GATEWAY_API_KEY" to "hunter2",
        "API_GATEWAY_URL" to "http://localhost/",
        "regel.api.secret" to "supersecret",
        "regel.api.key" to "regelKey",
        "GRAPH_QL_KEY" to "hunter2",
        "forskudd.api.key" to "hunter2"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "application.httpPort" to "8099",
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "loginservice.idporten.discovery.url" to "https://login.microsoftonline.com/navnob2c.onmicrosoft.com/discovery/v2.0/keys?p=b2c_1a_idporten",
        "loginservice.idporten.audience" to "https://login.microsoftonline.com/8b7dfc8b-b52e-4741-bde4-d83ea366f94f/v2.0/",
        "application.profile" to "PROD",
        "application.httpPort" to "8099",
    )
)

data class Configuration(
    val application: Application = Application(),
    val auth: Auth = Auth()
) {
    class Auth(
        regelApiSecret: String = config()[Key("regel.api.secret", stringType)],
        regelApiKeyPlain: String = config()[Key("regel.api.key", stringType)]
    ) {
        val regelApiKey = ApiKeyVerifier(regelApiSecret).generate(regelApiKeyPlain)
    }

    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)],
        val jwksUrl: String = config()[Key("loginservice.idporten.discovery.url", stringType)],
        val jwksIssuer: String = config()[Key("loginservice.idporten.audience", stringType)],
        val name: String = "dp-kalkulator-api",
        val apiGatewayBaseUrl: String = config()[Key("API_GATEWAY_URL", stringType)],
        val apiGatewayKey: String = config()[Key("API_GATEWAY_API_KEY", stringType)],
        val graphQlBaseUrl: String = config()[Key("API_GATEWAY_URL", stringType)] + "dp-graphql/graphql/",
        val regelApiBaseUrl: String = config()[Key("API_GATEWAY_URL", stringType)] + "dp-regel-api",
        val graphQlKey: String = config()[Key("GRAPH_QL_KEY", stringType)],
        val forskuddApiKey: String = config()[Key("forskudd.api.key", stringType)],
        val basePath: String = config().getOrElse(Key("app.basepath", stringType), "/arbeid/dagpenger/kalkulator-api")

    )
}

enum class Profile {
    LOCAL, DEV, PROD
}

fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-sbs" -> ConfigurationProperties.systemProperties() overriding devProperties overriding EnvironmentVariables
    "dev-gcp" -> ConfigurationProperties.systemProperties() overriding devProperties overriding EnvironmentVariables
    "prod-sbs" -> ConfigurationProperties.systemProperties() overriding prodProperties overriding EnvironmentVariables
    "prod-gcp" -> ConfigurationProperties.systemProperties() overriding prodProperties overriding EnvironmentVariables
    else -> {
        ConfigurationProperties.systemProperties() overriding localProperties overriding EnvironmentVariables
    }
}
