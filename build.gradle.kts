plugins {
    java
}

group = "com.circuitdrop"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Server is Paper 26.2 (calendar-versioned Minecraft). Its paper-api artifact declares
    // a minimum JVM target of 25 in its Gradle module metadata, so the build must use 25 too.
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
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
