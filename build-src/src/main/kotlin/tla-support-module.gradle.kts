plugins {
    id("minecraft-mod")
}

project.evaluationDependsOn(":core")

val recipeModExtension = extensions.create<RecipeModExtension>("tla")

val fabricModJson = project.tasks.register<FabricModJsonTask>("fabricModJson") {
    dependsOn(project.tasks.getByPath(":core:processResources"))
    inputFile = project(":core").layout.buildDirectory.file("resources/main/fabric.mod.json")
    outputFile = File("src/generated/resources/fabric.mod.json")
    modid = "tla-api-${recipeModExtension.modid.get()}-support"
    displayname = "TLA-Api ${recipeModExtension.modid.get().uppercase()} Support Extension"
    modDescription = "Adds support for ${recipeModExtension.displayname.get()} using TLA API"
    entrypoints = recipeModExtension.entrypoints.get()
    mixins.set(recipeModExtension.mixins)
    appendMode = false

    description = "Create FMJ file for TLA Support Module ${project.name}"
    group = "fabric"
}

val neoforgeModsToml = project.tasks.register<TomlFileTask>("neoforgeModsToml") {
    outputFile = File("src/generated/resources/META-INF/neoforge.mods.toml")
    mainSection.set(mapOf(
        "license" to "MIT",
        "modid" to "tla-api-${recipeModExtension.modid.get()}-support",
        "version" to rootProject.version.toString(),
        "displayName" to "TLA-Api ${recipeModExtension.modid.get().uppercase()} Support Extension",
        "authors" to "MattiDragon, Sollace",
        "description" to "Adds support for ${recipeModExtension.displayname.get()} using TLA API"
    ))
    recipeModExtension.entrypoints.get().forEach { entry : Map.Entry<String, String> ->
        mainSection.put(entry.key, entry.value)
    }
    recipeModExtension.mixins.get().forEach { mixin : String ->
        sections.add("mixins" to mapOf("config" to "${mixin}.mixin.json"))
    }
    sections.add("dependencies.tla-api" to mapOf(
        "modId" to "neoforge",
        "type" to "required",
        "ordering" to "NONE",
        "side" to "BOTH"
    ))
    sections.add("dependencies.tla-api" to mapOf(
        "modId" to "minecraft",
        "type" to "required",
        "ordering" to "NONE",
        "side" to "BOTH"
    ))

    group = "neoforge"
    description = "Create the (N)FMT file for TLA Support Module ${project.name}"
}

tasks.clean {
    doLast {
        delete(fabricModJson.flatMap{ it.outputFile })
    }
}

sourceSets {
    create("generated")
}

project.loom {
    runs {
        create("test_client") {
            inherit(runs["client"])
            configName = "Test Minecraft Client"
            source(sourceSets["test"])
        }
    }

    listOf("client", "test_client").forEach {
        runs[it].ideConfigGenerated(true)
        // Direct runs to the root project's shared directory
        runs[it].runDir("../../run")
    }
}

project.tasks.jar {
    from(fabricModJson.flatMap{ it.outputFile })
}

project.tasks.processResources {
    dependsOn(fabricModJson)
    dependsOn(neoforgeModsToml)
}

project.tasks.remapJar {
    archiveClassifier.set("fabric")

    destinationDirectory.set(project.layout.buildDirectory.map{ it.dir("devlibs") })
}

val neoforgeRemapJar = project.tasks.register<Jar>("neoforgeJar") {
    dependsOn(project.tasks.remapJar)
    from(zipTree((project.tasks.getByName("remapJar") as AbstractArchiveTask).archiveFile)) {
        exclude("fabric.mod.json")
    }
    into("META-INF") {
      from(neoforgeModsToml.flatMap{ it.outputFile })
    }

    archiveClassifier.set("neoforge")
    destinationDirectory.set(project.layout.buildDirectory.map{ it.dir("devlibs") })

    description = "Repackage production jar for neoforge"
    group = "neoforge"
}

project.tasks.assemble {
    dependsOn(neoforgeRemapJar)
}

dependencies {
    runtimeOnly(project(":testmod", "namedElements"))
    runtimeOnly(project(":fabric", "namedElements"))
}

