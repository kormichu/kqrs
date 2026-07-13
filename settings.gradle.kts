plugins {
    id("com.gradleup.nmcp.settings") version "1.6.1"
}

nmcpSettings {
    centralPortal {
        username = System.getenv("OSSRH_USERNAME") ?: settings.extra.properties["ossrh.username"] as String? ?: ""
        password = System.getenv("OSSRH_PASSWORD") ?: settings.extra.properties["ossrh.password"] as String? ?: ""
        publishingType = "AUTOMATIC"
    }
}

rootProject.name = "kqrs"

include("kqrs-core")
include("kqrs-reactor")
include("kqrs-prometheus")
include("kqrs-spring")
include("kqrs-spring-boot-autoconfigure")
include("kqrs-spring-boot-reactor-autoconfigure")

include("kqrs-spring-boot-prometheus-autoconfigure")
