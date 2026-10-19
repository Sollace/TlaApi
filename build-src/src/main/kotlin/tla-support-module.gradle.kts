plugins {
    id("fabric-loom")
}

project.java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}
project.evaluationDependsOn(":core")

val minecraftVersion = rootProject.property("minecraft_version") as String
val fabricLoaderVersion = rootProject.property("loader_version") as String
val yarnMappings = rootProject.property("yarn_mappings") as String
val fabricVersion = rootProject.property("fabric_version") as String

System.out.println("Minecraft Version: ${minecraftVersion}, Fabric Version: ${fabricVersion}, Loader Version: ${fabricLoaderVersion}")

val recipeModExtension = extensions.create<RecipeModExtension>("tla")

val fabricModJson = project.tasks.register<FabricModJsonTask>("fabricModJson") {
    inputFile = file("${rootDir}/core/src/main/resources/fabric.mod.json")
    outputFile = File(project.layout.buildDirectory.getAsFile().get(), "module_gensources/fabric.mod.json")
    modid = "tla-api-${recipeModExtension.modid.get()}-support"
    displayname = "TLA-Api ${recipeModExtension.modid.get().uppercase()} Support Extension"
    modDescription = "Adds support for ${recipeModExtension.displayname.get()} using TLA API"
    entrypoints = recipeModExtension.entrypoints.get()
}

project.version = rootProject.version
project.group = rootProject.group

sourceSets {
    test {
        java {
            setSrcDirs(listOf("${rootDir}/core/src/test/java"))
        }
        resources {
            setSrcDirs(listOf("${rootDir}/core/src/test/resources"))
        }
    }
}

project.loom {
    runs {
        create("test_client") {
            inherit(runs["client"])
            configName = "Test Minecraft Client"
            source(sourceSets["test"])
        }
    }
    runs["client"].ideConfigGenerated(true)
    runs["test_client"].ideConfigGenerated(true)
}

project.tasks.jar {
    dependsOn(fabricModJson)
    from(fabricModJson.flatMap{ it.outputFile })
}

project.tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(
          "version" to project.version.toString(),
          "loader_version" to fabricLoaderVersion,
          "fabric_version" to fabricVersion,
          "minecraft_version" to minecraftVersion
        )
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    implementation(project(":core", "namedElements"))
}

configurations.all {
    resolutionStrategy.force("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
}

