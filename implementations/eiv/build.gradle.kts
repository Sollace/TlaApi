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
    maven("https://api.modrinth.com/maven")
}*/

dependencies {
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    minecraft(rootProject.libs.minecraft)
    mappings(rootProject.libs.fabric.yarn.withClassifier("v2"))
    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)

    modImplementation(project(":core", "namedElements"))

    mapped("https://api.modrinth.com/maven", rootProject.libs.eiv.fabric) { path -> 
        path.contains("/api/")
         || path.contains("RecipeViewMenu")
         || path.contains("RecipeViewScreen")
         || path.contains("OptionalSlotRenderer")
         || path.contains("ItemViewRecipes")
         || path.contains("SlotContent")
         || path.contains("/builtin/")
         || path.contains("FluidStack")
    }
    modCompileOnly(rootProject.libs.eiv.api.fabric)
    modRuntimeOnly(rootProject.libs.eiv.fabric)
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
