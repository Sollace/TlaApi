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
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("fabric-loom:fabric-loom.gradle.plugin:1.10-SNAPSHOT")
    implementation("org.ajoberstar.reckon:org.ajoberstar.reckon.gradle.plugin:0.13.1")
}
