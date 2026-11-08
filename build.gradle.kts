plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("java")
    id("maven-publish")
}

group = "com.mcchopik"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net") }
    maven { url = uri("https://maven.modrinth.org") }
}

dependencies {
    minecraft "com.mojang:minecraft:1.21.4"
    mappings "net.fabricmc:yarn:1.21.4+build.1:v2"
    modImplementation "net.fabricmc:fabric-loader:0.16.9"
    modApi "net.fabricmc.fabric-api:fabric-api:0.100.0+1.21.4"
    modImplementation "com.google.code.gson:gson:2.11.0"
}

loom {
    mappings = "yarn"
    accessWidener = file("src/main/resources/jinfo.accesswidener")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named("jar") {
    manifest {
        attributes(
            "Main-Class" to "com.mcchopik.jinfo.JInfoMod",
            "Fabric-Mod" to "true",
            "Mod-Id" to "jinfo",
            "Mod-Name" to "JInfo",
            "Mod-Version" to project.version.toString()
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.filter { it.name.endsWith(".jar") }.map { zipTree(it) })
}