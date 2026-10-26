plugins {
    id("loader-variant")
    id("split-sources")
}

loom {
    mods {
        create("tla-api") {
            sourceSet(sourceSets.getByName("main"))
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

dependencies {
    mappings(variantOf(rootProject.libs.fabric.yarn) { classifier("v2") })
    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)
}

configurations.all {
    resolutionStrategy {
        force(rootProject.libs.fabric.loader)
        force(rootProject.libs.fabric.api)
    }
}
