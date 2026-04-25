plugins {
    id("java-library")
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(libs.spring.boot.starter.web)
    api(libs.spring.tx)
    api(libs.spring.data.commons)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.slf4j)
    api(libs.reactor.core)
    api(libs.kotlin.reflect)
    api(libs.slf4j.api)
    api(libs.micrometer.core)
    api(libs.uuid.creator)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.wiremock.standlone)
    testImplementation(libs.reactor.test)
    testImplementation(libs.mockk)
    testImplementation(libs.logbook.spring.boot.webflux.autoconfigure)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertk.jvm)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
