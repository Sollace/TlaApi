
dependencies {
    implementation(project(path = ":core", configuration = "namedElements"))
    implementation(coreProject.sourceSets.client.output)

    modCompileOnly(variantOf(rootProject.libs.emi) { classifier("api") })
    modRuntimeOnly(rootProject.libs.emi)
}

