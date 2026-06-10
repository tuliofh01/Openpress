plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "org.nexus.openpress"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.compression)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.sessions)
    implementation(libs.h2database.h2)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)
    implementation(libs.ucasoft.ktorSimpleCache)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
