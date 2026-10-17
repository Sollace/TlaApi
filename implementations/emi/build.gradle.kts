
dependencies {
    implementation(path: project(":core"), configuration: "namedElements")
    implementation project(":core").sourceSets.client.output

    modCompileOnly(rootProject.libs.emi.withClassifier("api"))
    modRuntimeOnly(rootProject.libs.emi)
}

