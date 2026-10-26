import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.ZipUtils

plugins {
    id("org.relativitymc.neo-loom-remap")
    id("minecraft-library")
}

java {
    withSourcesJar()
}

base {
    archivesName = "${rootProject.name}-${project.name}"
}

val enabledRecipeModModules = (rootProject.property("enabled_recipe_mod_modules") as String).split(" ")

enabledRecipeModModules.filter { it.length > 0 }.forEach { impl ->
    val remapMojJarTask = project.tasks.register<RemapJarTask>("remapMojJar${impl}") {
        val jarTask = project.tasks.getByPath(":implementations:${impl}:neoforgeJar") as AbstractArchiveTask
        dependsOn(jarTask)
        inputFile.set(jarTask.archiveFile)

        sourceNamespace.set("named")

        archiveClassifier.set("include-${impl}-remapped")
        destinationDirectory.set(project.layout.buildDirectory.map{ it.dir("includeJars") })

        description = "Remaps dependency jar to project local mappings."
        group = "neoforge"
    }

    project.tasks.remapJar {
        dependsOn(remapMojJarTask)
        addNestedDependencies.set(true)
        nestedJars.from(remapMojJarTask
          .flatMap{ it.archiveFile }
          .map {
              ZipUtils.add(it.getAsFile().toPath(), "META-INF/neo-loom-nesting-metadata.json", "{\"group\": \"${project.group}\", \"name\": \"${impl}\", \"version\": \"${project.version}\" }")
              it
          })
    }
}

dependencies {
    implementation(project(":core", "namedElements"))
}

tasks.jar {
    dependsOn(":core:jar")
    from(zipTree("${rootDir}/core/build/devlibs/core-${version}-dev.jar")) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.named("remapSourcesJar", AbstractArchiveTask::class.java) {
    dependsOn(":core:remapSourcesJar")
    from(zipTree("${rootDir}/core/build/libs/core-${version}-sources.jar")) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier = "api"
    dependsOn(tasks.remapJar)
    from(zipTree(tasks.remapJar.flatMap{ it.archiveFile }))
    include("io/github/mattidragon/tlaapi/api/**")
    include("LICENSE")
}

tasks.assemble {
    dependsOn(apiJar)
}
