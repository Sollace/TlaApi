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
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    modCompileOnly(rootProject.libs.emi.withClassifier("api"))
    modRuntimeOnly(rootProject.libs.emi)
}
