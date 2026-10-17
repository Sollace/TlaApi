plugins {
    `mapped-dependency-helper`
}

dependencies {
    implementation(project(":core"))

    mapped.dependency("https://api.modrinth.com/maven", rootProject.libs.eiv.fabric) { path -> path.contains("/api/") }
    modCompileOnly(rootProject.libs.eiv.api.fabric)
    modRuntimeOnly(rootProject.libs.eiv.fabric)
}
