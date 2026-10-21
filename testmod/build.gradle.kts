plugins {
    id("minecraft-mod")
}

dependencies {
    implementation(project(":core", "namedElements"))
}

configurations.all {
    resolutionStrategy.force(rootProject.libs.fabric.loader)
}

