plugins {
    id("java-library")
}

dependencies {
    api(project(":kqrs-core"))
    api(libs.micrometer.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertk.jvm)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
