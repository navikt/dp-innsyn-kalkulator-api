package no.nav.dagpenger.kalkulator

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier

private val localProperties = ConfigurationMap(
    mapOf(
        "API_GATEWAY_API_KEY" to "hunter2",
        "API_GATEWAY_URL" to "http://localhost/",
        "PDL_API_SCOPE" to "api://dev-fss.pdl.pdl-api/.default",
        "PDL_API_URL" to "https://pdl-api.dev-fss-pub.nais.io/graphql",
        "DP_PROXY_SCOPE" to "api://dev-fss.teamdagpenger.dp-proxy/.default",
        "DP_PROXY_URL" to "https://dp-proxy.dev-fss-pub.nais.io",
        "application.httpPort" to "8099",
        "application.profile" to "LOCAL",
        "forskudd.api.key" to "hunter2",
        "jwks.issuer" to "https://localhost",
        "jwks.issuer.ny" to "",
        "jwks.url" to "https://localhost",
        "jwks.url.ny" to "",
        "loginservice.idporten.discovery.url" to "https://localhost",
        "regel.api.key" to "regelKey",
        "regel.api.secret" to "supersecret"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "PDL_API_SCOPE" to "api://dev-fss.pdl.pdl-api/.default",
        "PDL_API_URL" to "https://pdl-api.dev-fss-pub.nais.io/graphql",
        "DP_PROXY_SCOPE" to "api://dev-fss.teamdagpenger.dp-proxy/.default",
        "DP_PROXY_URL" to "https://dp-proxy.dev-fss-pub.nais.io",
        "application.httpPort" to "8099",
        "application.profile" to "DEV",
        "jwks.issuer" to "https://oidc-ver2.difi.no/idporten-oidc-provider/",
        "jwks.issuer.ny" to "https://oidc-ver2.difi.no/idporten-oidc-provider/",
        "jwks.url" to "https://oidc-ver2.difi.no/idporten-oidc-provider/jwk",
        "jwks.url.ny" to "https://oidc-ver2.difi.no/idporten-oidc-provider/jwk",
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "PDL_API_SCOPE" to "api://prod-fss.pdl.pdl-api/.default",
        "PDL_API_URL" to "https://pdl-api.prod-fss-pub.nais.io/graphql",
        "DP_PROXY_SCOPE" to "api://prod-fss.teamdagpenger.dp-proxy/.default",
        "DP_PROXY_URL" to "https://dp-proxy.prod-fss-pub.nais.io",
        "application.httpPort" to "8099",
        "application.profile" to "PROD",
        "jwks.issuer" to "https://oidc.difi.no/idporten-oidc-provider/",
        "jwks.issuer.ny" to "https://oidc.difi.no/idporten-oidc-provider/",
        "jwks.url" to "https://oidc.difi.no/idporten-oidc-provider/jwk",
        "jwks.url.ny" to "https://oidc.difi.no/idporten-oidc-provider/jwk",
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
        val idportenDiscoveryUrl: String = config()[Key("loginservice.idporten.discovery.url", stringType)],
        val jwksUrl: String = config()[Key("jwks.url", stringType)],
        val jwksIssuer: String = config()[Key("jwks.issuer", stringType)],
        val jwksUrlNy: String = config()[Key("jwks.url.ny", stringType)],
        val jwksIssuerNy: String = config()[Key("jwks.issuer.ny", stringType)],
        val name: String = "dp-kalkulator-api",

        val pdlApiBaseUrl: String = config()[Key("PDL_API_URL", stringType)],
        val forskuddApiKey: String = config()[Key("forskudd.api.key", stringType)],
        val basePath: String = config().getOrElse(Key("app.basepath", stringType), "/arbeid/dagpenger/kalkulator-api"),
        val dpProxyUrl: String = config()[Key("DP_PROXY_URL", stringType)] + "/proxy/v1/regelapi",
    ) {
        fun tokenProvider() = ClientCredentialsClient(config()) {
            scope {
                add(config()[Key("PDL_API_SCOPE", stringType)])
            }
        }

        fun dpProxyTokenProvider() = ClientCredentialsClient(config()) {
            scope {
                add(config()[Key("DP_PROXY_SCOPE", stringType)])
            }
        }
    }
}

enum class Profile {
    LOCAL, DEV, PROD
}

fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-gcp" -> ConfigurationProperties.systemProperties() overriding devProperties overriding EnvironmentVariables
    "prod-gcp" -> ConfigurationProperties.systemProperties() overriding prodProperties overriding EnvironmentVariables
    else -> {
        ConfigurationProperties.systemProperties() overriding localProperties overriding EnvironmentVariables
    }
}
