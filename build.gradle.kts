plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10-RC"
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.horizons_server.golf"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://maven.enginehub.org/repo") }
//    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.spigotmc:spigot-api:1.19-R0.1-SNAPSHOT")
//    compileOnly("com.sk89q.worldguard:worldguard-bukkit:1.19")
}

