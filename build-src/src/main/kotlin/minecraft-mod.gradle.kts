plugins {
    id("minecraft-library")
}

val yarnMappings = rootProject.property("yarn_mappings") as String
val loaderVersion = rootProject.property("loader_version") as String
val fabricVersion = rootProject.property("fabric_version") as String

dependencies {
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    implementation(project(":core", "namedElements"))
}

configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${loaderVersion}")
        force("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
    }
}
