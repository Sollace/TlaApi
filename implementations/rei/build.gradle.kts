plugins {
    id("tla-support-module")
    id("split-sources")
}

/*repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
}*/

tla {
    modid = "rei"
    displayname = "Roughly Enough Items"
    entrypoints.putAll(mapOf(
        "rei_client" to "io.github.mattidragon.tlaapi.impl.client.rei.TlaApiReiClientPlugin",
        "rei_common" to "io.github.mattidragon.tlaapi.impl.rei.TlaApiReiCommonPlugin"
    ))
}

dependencies {
    modCompileOnly(rootProject.libs.rei.api)
    modCompileOnly(rootProject.libs.rei.plugin.default)
    // For some reason arch isn't a transitive dependency of rei-api, so we need to manually add it to use a few classes
    modCompileOnly(rootProject.libs.rei.architectury)
    modCompileOnly(rootProject.libs.rei.config)

    modRuntimeOnly(rootProject.libs.rei.all)
}
