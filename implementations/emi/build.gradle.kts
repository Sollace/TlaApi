plugins {
    id("tla-support-module")
    id("mapped-dependency")
}

/*repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
}*/

tla {
    modid = "emi"
    displayname = "EMI"
    entrypoints.putAll(mapOf("emi" to "io.github.mattidragon.tlaapi.impl.emi.TlaApiEmiPlugin"))
}

dependencies {
    modCompileOnly(variantOf(rootProject.libs.emi) { classifier("api") })
    modRuntimeOnly(rootProject.libs.emi)
}
