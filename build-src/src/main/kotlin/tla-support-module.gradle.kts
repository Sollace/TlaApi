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
    appendMode = false
    
    description = "Create FMJ file for TLA Support Module"
}

tasks.clean {
    doLast {
        delete(fabricModJson.flatMap{ it.outputFile })
    }
}

sourceSets {
    create("generated") {
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
}

dependencies {
    implementation(project(":core", "namedElements"))
    testImplementation(project(":testmod", "namedElements"))
}

