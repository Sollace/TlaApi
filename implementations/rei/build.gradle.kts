loom {
    splitEnvironmentSourceSets()
}

dependencies {
    implementation(path: project(":core"), configuration: "namedElements")
    implementation project(":core").sourceSets.client.output

    modCompileOnly(rootProject.libs.rei.api)
    modCompileOnly(rootProject.libs.rei.plugin.default)
    // For some reason arch isn't a transitive dependency of rei-api, so we need to manually add it to use a few classes
    modCompileOnly(rootProject.libs.rei.architectury)
    modCompileOnly(rootProject.libs.rei.config)

    modRuntimeOnly(rootProject.libs.rei.all)
}
