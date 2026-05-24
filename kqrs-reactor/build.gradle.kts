plugins {
    id("java-library")
}

dependencies {
    api(project(":kqrs-core"))
    api(libs.reactor.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.assertk.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.reactor.test)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
