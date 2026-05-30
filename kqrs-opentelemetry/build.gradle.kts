plugins {
    id("java-library")
}

dependencies {
    api(project(":kqrs-core"))
    api(libs.opentelemetry.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertk.jvm)
    testImplementation(libs.opentelemetry.sdk.testing)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
