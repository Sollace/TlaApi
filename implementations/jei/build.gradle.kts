plugins {
    id("mapped-dependency")
}

val coreProject = project(":core")
dependencies {
    implementation(project(path = ":core", configuration = "namedElements"))
    implementation(coreProject.sourceSets.getByName("client").output)

    // Because of issues with the remapping of the jei api, we have to depend on the fat jar instead (https://github.com/mezz/JustEnoughItems/issues/2891)
    mapped("https://maven.blamejared.com", rootProject.libs.jei.fabric) { path -> path.contains("/api/") || path.contains("DrawableAnimated") }
    modCompileOnly(rootProject.libs.jei.api.fabric)
    modRuntimeOnly(rootProject.libs.jei.fabric)
}
