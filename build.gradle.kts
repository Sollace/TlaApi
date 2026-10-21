plugins {
    id("maven-publish")
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("split-sources")
    //id("org.ajoberstar.reckon") version "0.13.1"
}

val minecraftVersion = property("minecraft_version") as String
val yarnMappings = property("yarn_mappings") as String
val loaderVersion = property("loader_version") as String
val fabricVersion = property("fabric_version") as String
val enabledRecipeModModules = (property("recipe_mod_modules") as String).split(" ")

version = "${property("mod_version")}+${minecraftVersion}"
group = "io.github.mattidragon"

subprojects {
    repositories {
        flatDir({ dirs(File(rootDir, "lib")) })

        maven("https://api.modrinth.com/maven")
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.blamejared.com/")
    }
}

val SourceSetContainer.client: SourceSet
    get() = this["client"]
val SourceSetContainer.main: SourceSet
    get() = this["main"]

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

dependencies {
    minecraft(rootProject.libs.minecraft)
    mappings(variantOf(rootProject.libs.fabric.yarn) { classifier("v2") })

    enabledRecipeModModules.forEach { impl ->
        if (impl.length > 0) {
            implementation(project(path = ":implementations:${impl}", configuration = "namedElements"))
            include(project(":implementations:${impl}"))
        }
    }
}

tasks.processResources  {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(
          "version" to project.version,
          "loader_version" to loaderVersion,
          "fabric_version" to fabricVersion,
          "minecraft_version" to minecraftVersion
        )
    }
}

tasks.jar {
    dependsOn(":core:remapJar")
    from(zipTree("${rootDir}/core/build/libs/core-${version}.jar")) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    from(File(rootDir, "LICENSE"))
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier = "api"
    dependsOn(tasks.jar)
    from(zipTree(tasks.jar.flatMap{ it.archiveFile }))
    include("io/github/mattidragon/tlaapi/api/**")
}

tasks.build {
    dependsOn(apiJar)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
    }
}
