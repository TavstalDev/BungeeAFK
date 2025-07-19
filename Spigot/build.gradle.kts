plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.maven.apache.org/maven2/")
}

dependencies {
    implementation(project(":bungeeafk-core"))
    compileOnly(libs.spigot)
    implementation(libs.adventureTextMinimessage)
    implementation(libs.annotations)
    implementation(libs.adventureBukkit)
    implementation(libs.guice)
    implementation(libs.bstats)
}

group = "net.fameless"
version = "1.1.0"
description = "BungeeAFK for Spigot servers"
java.sourceCompatibility = JavaVersion.VERSION_21

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveBaseName.set("BungeeAFK-Spigot")
        archiveClassifier.set("")
        archiveVersion.set("1.1.0")

        relocate("org.bstats", "net.fameless.bungeeafk.bstats")
    }

    jar {
        enabled = false
    }
}
