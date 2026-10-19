plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("mapped-dependency")
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

/*repositories {
    maven("https://maven.blamejared.com/")
}*/

dependencies {
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    minecraft(rootProject.libs.minecraft)
    mappings(rootProject.libs.fabric.yarn.withClassifier("v2"))
    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)

    modImplementation(project(":core", "namedElements"))

    // Because of issues with the remapping of the jei api, we have to depend on the fat jar instead (https://github.com/mezz/JustEnoughItems/issues/2891)
    mapped("https://maven.blamejared.com", rootProject.libs.jei.fabric) { path -> path.contains("/api/") || path.contains("DrawableAnimated") || path.contains("Internal") }
    modCompileOnly(rootProject.libs.jei.api.fabric)
    modRuntimeOnly(rootProject.libs.jei.fabric)
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
