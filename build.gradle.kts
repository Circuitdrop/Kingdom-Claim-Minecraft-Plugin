plugins {
    java
}

group = "com.circuitdrop"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Server is Paper 26.2 (calendar-versioned Minecraft, requires Java 25 at runtime).
    // Building against Java 21 bytecode keeps the plugin usable on any Paper build that ships that runtime.
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveBaseName.set("KingdomClaim")
    }
}
