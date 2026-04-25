plugins {
    id("java-library")
}

dependencies {
    api(libs.kotlin.reflect)
    api(libs.slf4j.api)
    api(libs.micrometer.core)
    api(libs.uuid.creator)

    testImplementation(libs.mockk)
    testImplementation(libs.assertk.jvm)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
