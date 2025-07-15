plugins {
    `maven-publish`
    id ("com.gradleup.shadow") version ("8.3.3")
    id("java")
}

group = "gg.crystalized.lobby"
version = "1.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("local") {
            groupId = "gg.crystalized.lobby"
            artifactId = project.name
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
		maven("https://repo.dmulloy2.net/repository/public/")
		maven("https://maven.citizensnpcs.co/repo")
    maven {url = uri("https://repo.opencollab.dev/main/") }
    maven {url = uri("https://jitpack.io")}
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("net.citizensnpcs:citizens-main:2.0.37-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")

    implementation("io.github.colonelparrot:jchessify:1.0.2")
    compileOnly ("com.github.bhlangonijr:chesslib:1.3.4")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }
}
tasks {
    build {
        dependsOn("shadowJar")
    }
}
