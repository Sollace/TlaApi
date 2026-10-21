plugins {
    id("fabric-loom")
}

if (project != rootProject) {
    // Root project configures reckon (only root project can do that)
    version = rootProject.version
}

group = rootProject.group

val minecraftVersion = rootProject.property("minecraft_version") as String
val yarnMappings = rootProject.property("yarn_mappings") as String
val loaderVersion = rootProject.property("loader_version") as String
val fabricVersion = rootProject.property("fabric_version") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
}

configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${loaderVersion}")
        force("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
    }
}

tasks.processResources  {
    inputs.property("version", project.version.toString())
    filesMatching("fabric.mod.json") {
        expand(
          "version" to project.version.toString(),
          "loader_version" to loaderVersion,
          "fabric_version" to fabricVersion,
          "minecraft_version" to minecraftVersion
        )
    }
}
