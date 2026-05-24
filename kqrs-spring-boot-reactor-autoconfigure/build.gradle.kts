plugins {
    id("java-library")
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(project(":kqrs-spring-boot-autoconfigure"))
    api(project(":kqrs-reactor"))
    api(libs.spring.boot.autoconfigure)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertk.jvm)
    testImplementation(libs.mockk)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.reactor.core)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
