import org.gradle.api.publish.PublishingExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.axion.release)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spring.dependency)
    `maven-publish`
    signing
    idea
}

scmVersion {
    tag {
        prefix.set(project.rootProject.name)
        versionSeparator.set("-")
    }
    versionCreator("versionWithBranch")
    releaseOnlyOnReleaseBranches = true
    releaseBranchNames.add("main")
}

allprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("org.jetbrains.kotlin.plugin.spring")
    plugins.apply("idea")
    plugins.apply("pl.allegro.tech.build.axion-release")

    project.group = "com.kormichu"
    project.version = scmVersion.version

    repositories {
        mavenLocal()
        mavenCentral()
    }
}
subprojects {
    plugins.apply("maven-publish")
    plugins.apply("signing")
    plugins.apply("pl.allegro.tech.build.axion-release")
    plugins.apply("io.spring.dependency-management")

    java {
        withJavadocJar()
        withSourcesJar()
    }


    configure<PublishingExtension> {
        publications {
            register<MavenPublication>("mavenJava") {
                from(components["java"])

                artifactId = project.name

                pom {
                    name.set(project.name)
                    description.set("${project.name} module for KQRS - Kotlin CQRS framework")
                    url.set("https://github.com/kormichu/kqrs")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("kormichu")
                            name.set("kormichu")
                            url.set("https://github.com/kormichu")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/kormichu/kqrs.git")
                        developerConnection.set("scm:git:ssh://github.com/kormichu/kqrs.git")
                        url.set("https://github.com/kormichu/kqrs")
                    }
                }
            }
        }
    }

    signing {
        val signingKey = System.getenv("GPG_SIGNING_KEY") ?: project.findProperty("signing.key") as String?
        val signingPassword = System.getenv("GPG_SIGNING_PASSWORD") ?: project.findProperty("signing.password") as String?
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
        sign(the<PublishingExtension>().publications["mavenJava"])
    }

    tasks.withType<Sign>().configureEach {
        isRequired = System.getenv("GPG_SIGNING_KEY") != null || project.hasProperty("signing.key")
    }

    pluginManager.withPlugin("java") {
        configure<SourceSetContainer> {
            named("main") {
                java.setSrcDirs(emptyList<String>())
            }
            named("test") {
                java.setSrcDirs(emptyList<String>())
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
        sourceSets["main"].apply {
            kotlin.srcDirs("src/main/kotlin")
        }
        sourceSets["test"].apply {
            kotlin.srcDirs("src/test/kotlin")
        }
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }
}


apply(from = rootProject.file("gradle/config/git/install-git-hooks.gradle"))
