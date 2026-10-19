plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("fabric-loom:fabric-loom.gradle.plugin:1.10-SNAPSHOT")
}
