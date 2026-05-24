plugins {
    id("java-library")
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(project(":kqrs-spring"))
    api(libs.spring.boot.autoconfigure)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertk.jvm)
    testImplementation(libs.mockk)
    testImplementation(libs.micrometer.core)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
