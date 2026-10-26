plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    maven("https://repo.codemc.io/repository/relativitymc/") {
        name = "RelativityMC"
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("fabric-loom:fabric-loom.gradle.plugin:1.10-SNAPSHOT")
    implementation("org.relativitymc.neo-loom-remap:org.relativitymc.neo-loom-remap.gradle.plugin:1.17-SNAPSHOT")
    implementation("org.ajoberstar.reckon:org.ajoberstar.reckon.gradle.plugin:0.13.1")
    implementation("io.github.dexman545.outlet:io.github.dexman545.outlet.gradle.plugin:1.9.0")
}
