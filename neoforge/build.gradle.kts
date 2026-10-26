plugins {
    id("neo-loader-variant")
}

version = rootProject.version
group = rootProject.group

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    forgeUserdev(variantOf(rootProject.libs.neoforge) { classifier("userdev") })
    mappings(loom.layered {
        mappings(variantOf(rootProject.libs.fabric.yarn) { classifier("v2") })
        mappings(rootProject.libs.fabric.yarn.neo)
    })
}
