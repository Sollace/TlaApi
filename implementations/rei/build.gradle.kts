plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
}

val minecraftVersion = rootProject.property("minecraft_version") as String
val yarnMappings = rootProject.property("yarn_mappings") as String
val loaderVersion = rootProject.property("loader_version") as String
val fabricVersion = rootProject.property("fabric_version") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()
}

repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
}

dependencies {
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    minecraft(rootProject.libs.minecraft)
    mappings(rootProject.libs.fabric.yarn.withClassifier("v2"))
    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)

    modImplementation(project(":core", "namedElements"))

    modCompileOnly(rootProject.libs.rei.api)
    modCompileOnly(rootProject.libs.rei.plugin.default)
    // For some reason arch isn't a transitive dependency of rei-api, so we need to manually add it to use a few classes
    modCompileOnly(rootProject.libs.rei.architectury)
    modCompileOnly(rootProject.libs.rei.config)

    modRuntimeOnly(rootProject.libs.rei.all)
}

tasks.processResources  {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(
          "version" to project.version,
          "loader_version" to loaderVersion,
          "fabric_version" to fabricVersion,
          "minecraft_version" to minecraftVersion
        )
    }
}
