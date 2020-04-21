import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

repositories {
    jcenter()
    maven(url = "http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

application {
    applicationName = "dp-kalkulator-api"
    mainClassName = "no.nav.dagpenger.kalkulator.KalkulatorAppKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val kotlinLoggingVersion = "1.6.22"
val fuelVersion = "2.1.0"
val confluentVersion = "4.1.2"
val kafkaVersion = "2.0.0"

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test-junit5"))
    testImplementation(Junit5.api)
    testRuntimeOnly(Junit5.engine)

    implementation(Ktor.server)
    implementation(Ktor.serverNetty)
    implementation(Ktor.auth)
    implementation(Ktor.authJwt) {
        exclude(group = "junit")
    }

    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.Logstash.logstashLayout)
    implementation(Kotlin.Logging.kotlinLogging)

    implementation(Dagpenger.Biblioteker.stsKlient)
    implementation(Dagpenger.Biblioteker.ktorUtils)
    implementation(Konfig.konfig)

    implementation(Moshi.moshi)
    implementation(Moshi.moshiAdapters)
    implementation(Moshi.moshiKotlin)
    implementation(Moshi.moshiKtor)

    implementation(Ktor.micrometerMetrics)
    implementation(Micrometer.prometheusRegistry)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.log4j2)

    implementation(Fuel.fuel)
    implementation(Fuel.fuelMoshi)

    testImplementation(kotlin("test"))
    testImplementation(Ktor.ktorTest)
    testImplementation(Junit5.api)
    testImplementation(KoTest.assertions)
    testImplementation(KoTest.runner)

    testImplementation(Mockk.mockk)

    testImplementation(Wiremock.standalone)
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("jar") {
    dependsOn("test")
}

/*tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}*/

tasks.withType<Wrapper> {
    gradleVersion = "6.0.1"
}
