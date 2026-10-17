
val coreProject = project(":core")
dependencies {
    implementation(project(path = ":core", configuration = "namedElements"))
    implementation(coreProject.sourceSets.getByName("client").output)

    modCompileOnly(variantOf(rootProject.libs.emi) { classifier("api") })
    modRuntimeOnly(rootProject.libs.emi)
}

