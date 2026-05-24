import org.gradle.kotlin.dsl.register

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.axion.release)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spring.dependency)
    `maven-publish`
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
    plugins.apply("kotlin")
    plugins.apply("kotlin-spring")
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
    plugins.apply("pl.allegro.tech.build.axion-release")
    plugins.apply("io.spring.dependency-management")

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/kormichu/kqrs")
                credentials {
                    username = System.getenv("USERNAME") ?: project.findProperty("gpr.user") as String?
                    password = System.getenv("TOKEN") ?: project.findProperty("gpr.key") as String?
                }
            }
        }

        publications {
            register<MavenPublication>("gpr") {
                from(components["java"])

                // Optional: customize artifact IDs
                artifactId = "kqrs-${project.name}"

                // Add proper POM information if needed
                pom {
                    name.set(project.name)
                    description.set("${project.name} module for KQRS")
                }
            }
        }
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
