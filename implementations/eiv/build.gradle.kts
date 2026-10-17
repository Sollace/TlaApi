plugins {
    id("mapped-dependency")
}

dependencies {
    implementation(project(path = ":core", configuration = "namedElements"))

    mapped("https://api.modrinth.com/maven", rootProject.libs.eiv.fabric) { path -> path.contains("/api/") }
    modCompileOnly(rootProject.libs.eiv.api.fabric)
    modRuntimeOnly(rootProject.libs.eiv.fabric)
}
