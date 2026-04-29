plugins {
    id("java-library")
}

dependencies {
    api(project(":kqrs-core"))
    api(libs.micrometer.core)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
