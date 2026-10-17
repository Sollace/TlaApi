plugins {
    id("mapped-dependency")
}

val coreProject = project(":core")
dependencies {
    implementation(project(path = ":core", configuration = "namedElements"))
    implementation(coreProject.sourceSets.getByName("client").output)

    mapped("https://api.modrinth.com/maven", rootProject.libs.eiv.fabric) { path -> path.contains("/api/") }
    modCompileOnly(rootProject.libs.eiv.api.fabric)
    modRuntimeOnly(rootProject.libs.eiv.fabric)
}
