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
}

dependencies {
    mapped("https://api.modrinth.com/maven", rootProject.libs.eiv.fabric) { path -> 
        path.contains("/api/")
         || path.contains("RecipeViewMenu")
         || path.contains("RecipeViewScreen")
         || path.contains("OptionalSlotRenderer")
         || path.contains("ItemViewRecipes")
         || path.contains("SlotContent")
         || path.contains("/builtin/")
         || path.contains("FluidStack")
    }
    modCompileOnly(rootProject.libs.eiv.api.fabric)
    modRuntimeOnly(rootProject.libs.eiv.fabric)
}
