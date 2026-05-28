plugins {
    id("java-library")
}

dependencies {
    api(libs.kotlin.reflect)
    api(libs.slf4j.api)
    api(libs.uuid.creator)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.slf4j)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.assertk.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
