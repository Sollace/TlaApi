plugins {
    id("minecraft-mod")
    id("split-sources")
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
