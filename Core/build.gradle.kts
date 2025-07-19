plugins {
    `java-library`
}

group = "net.fameless"
version = "1.1.0"
description = "Core features implementing the basic logic of BungeeAFK"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.annotations)
    implementation(libs.gson)
    implementation(libs.guice)
    implementation(libs.adventureTextMinimessage)
    implementation(libs.adventureTextSerializerLegacy)
}
