plugins {
    id("minecraft-library")
}

java {
    withSourcesJar()
}

base {
    archivesName = "${rootProject.name}-${project.name}"
}

val enabledRecipeModModules = (rootProject.property("enabled_recipe_mod_modules") as String).split(" ")

dependencies {
    implementation(project(":core", "namedElements"))

    enabledRecipeModModules.forEach { impl ->
        if (impl.length > 0) {
            include(project(":implementations:${impl}"))
        }
    }
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