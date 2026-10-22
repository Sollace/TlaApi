plugins {
    id("fabric-loom")
}

loom {
    splitEnvironmentSourceSets()
}

// Ensure the client resources and classes directory is created for projects even if they have nothing to put there
//(required for classpath resolution)
// Aka gradle is dumb.
fun createRequiredResourceDirs() {
    val buildDir = project.layout.buildDirectory.getAsFile().get()
    val dirs = listOf(
        File(buildDir, "resources/client"),
        File(buildDir, "classes/java/client")
    )
    dirs.forEach { it.mkdirs() }
}

tasks.eclipse {
    doLast { createRequiredResourceDirs() }
}

tasks.processResources  {
    doLast { createRequiredResourceDirs() }
}

tasks.classes {
    doLast { createRequiredResourceDirs() }
}

project.afterEvaluate {
    createRequiredResourceDirs()
}
