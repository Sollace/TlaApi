plugins {
    id("tla-support-module")
    id("mapped-dependency")
}

/*repositories {
    maven("https://maven.blamejared.com/")
}*/

tla {
    modid = "jei"
    displayname = "Just Enough Items"
    entrypoints.putAll(mapOf("jei_mod_plugin" to "io.github.mattidragon.tlaapi.impl.jei.TlaApiJeiPlugin"))
}

dependencies {
    // Because of issues with the remapping of the jei api, we have to depend on the fat jar instead (https://github.com/mezz/JustEnoughItems/issues/2891)
    modCompileOnly(apiDependency(
        "https://maven.blamejared.com",
        rootProject.libs.jei.fabric,
        ".*(/api/|DrawableAnimated|Internal).*"
    ))
    modRuntimeOnly(rootProject.libs.jei.fabric)
}
