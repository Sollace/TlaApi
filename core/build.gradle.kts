plugins {
    id("minecraft-library")
    id("split-sources")
}

val fabricVersion = rootProject.property("fabric_version") as String

dependencies {
    mappings(variantOf(rootProject.libs.fabric.yarn) { classifier("v2") })
    // Depend on the minimal amount of dependencies needed to build
    modImplementation(rootProject.libs.fabric.loader)
}

configurations.all {
    resolutionStrategy {
        force(rootProject.libs.fabric.loader)
        //force(rootProject.libs.fabric.api)
    }
}

java {
    withSourcesJar()
}

val SourceSetContainer.client: SourceSet
    get() = this["client"]
val SourceSetContainer.main: SourceSet
    get() = this["main"]

loom {
    mods {
        create("tla-api") {
            sourceSet(sourceSets.main)
            sourceSet(sourceSets.client)
        }
    }
}

tasks.getByName<Jar>("sourcesJar") {
    exclude("io/github/mattidragon/tlaapi/impl/**")
    from(File(rootDir, "LICENSE"))
}
