plugins {
    id("tla-support-module")
    id("mapped-dependency")
}

/*repositories {
    maven("https://api.modrinth.com/maven")
}*/

tla {
    modid = "eiv"
    displayname = "Extended Item View"
    entrypoints.putAll(mapOf("eiv" to "io.github.mattidragon.tlaapi.impl.eiv.TlaApiEivPlugin"))
    mixins.add("tlaeiv")
}

loom {
    mixin.defaultRefmapName = "tlaeiv.mixin.refmap.json"
}

dependencies {
    modCompileOnly(apiDependency(
        "https://api.modrinth.com/maven",
        rootProject.libs.eiv.fabric,
        ".*(/api/|/builtin/|RecipeViewMenu|RecipeViewScreen|OptionalSlotRenderer|ItemViewRecipes|SlotContent|FluidStack|BlockingGuiComponent|OverlayManager|ItemFilters).*"
    ))
    modRuntimeOnly(rootProject.libs.eiv.fabric)
}
