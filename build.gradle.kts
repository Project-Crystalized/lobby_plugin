plugins {
    id("java")
}

group = "gg.crystalized.lobby"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
		maven("https://repo.dmulloy2.net/repository/public/")
		maven("https://maven.citizensnpcs.co/repo")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
		implementation("org.xerial:sqlite-jdbc:3.47.0.0")
		compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("net.citizensnpcs:citizens-main:2.0.37-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

