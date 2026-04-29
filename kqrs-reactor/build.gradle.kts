plugins {
    id("java-library")
}

dependencies {
    api(project(":kqrs-core"))
    api(libs.reactor.core)

    testImplementation(libs.reactor.test)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
