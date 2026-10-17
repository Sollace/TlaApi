import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    id("maven-publish")
    id("org.ajoberstar.reckon") version "0.13.1"
}

val minecraftVersion = property("minecraft_version") as String

version = "${property("mod_version")}+${minecraftVersion}"
group = "io.github.mattidragon"

subprojects {
    version = "${property("mod_version")}+${minecraftVersion}"
    group = "io.github.mattidragon"

    repositories {
        flatDir({ dirs(File(rootDir, "lib")) })
    }

    configurations.all {
        resolutionStrategy.force(rootProject.libs.fabric.loader)
    }
}
