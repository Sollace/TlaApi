plugins {
    id("fabric-loom")
}

loom {
    splitEnvironmentSourceSets()
}

// Ensure the client resources directory is created for projects with no client resources (required for classpath resolution)
// Aka gradle is dumb.

fun initClientResources() {
    val resourceDir = File(project.layout.buildDirectory.getAsFile().get(), "resources/client")
    if (!resourceDir.exists()) {
        resourceDir.mkdirs()
    }
}

tasks.eclipse {
    doLast {
        initClientResources()
    }
}

tasks.processResources  {
    doLast {
        initClientResources()
    }
}

project.afterEvaluate {
    initClientResources()
}
