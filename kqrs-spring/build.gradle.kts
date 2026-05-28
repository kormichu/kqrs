plugins {
    id("java-library")
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(project(":kqrs-core"))
    api(libs.spring.context)
    api(libs.spring.tx)
    api(libs.spring.data.commons)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testImplementation(libs.assertk.jvm)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
