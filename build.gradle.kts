import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    id("maven-publish")
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("org.ajoberstar.reckon") version "0.13.1"
}

val minecraftVersion = property("minecraft_version") as String
val yarnMappings = property("yarn_mappings") as String
val loaderVersion = property("loader_version") as String
val fabricVersion = property("fabric_version") as String
val enabledRecipeModModules = (property("recipe_mod_modules") as String).split(" ")

version = "${property("mod_version")}+${minecraftVersion}"
group = "io.github.mattidragon"

subprojects {
    version = rootProject.version
    group = rootProject.group
    
    apply(plugin = "java")

    repositories {
        flatDir({ dirs(File(rootDir, "lib")) })

        maven("https://api.modrinth.com/maven")
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.blamejared.com/")
    }

    configurations.all {
        resolutionStrategy.force(rootProject.libs.fabric.loader)
    }

    sourceSets {
        test {
            java {
                setSrcDirs(listOf("${rootDir}/core/src/test/java"))
            }
            resources {
                setSrcDirs(listOf("${rootDir}/core/src/test/resources"))
            }
        }
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

loom {
    splitEnvironmentSourceSets()
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("core/src/main/java"))
        }
        resources {
            setSrcDirs(listOf("core/src/main/resources"))
        }
    }

    named("client") {
        java {
            setSrcDirs(listOf("core/src/client/java"))
        }
    }
}

loom {
    mods {
        create("tla-api") {
            sourceSet(sourceSets.main)
            sourceSet(sourceSets.client)
        }
    }
}

dependencies {
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    minecraft(rootProject.libs.minecraft)
    mappings(rootProject.libs.fabric.yarn.withClassifier("v2"))
    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)

    implementation(project(path = ":core", configuration = "namedElements"))

    enabledRecipeModModules.forEach { impl ->
        implementation(project(path = ":implementations:${impl}", configuration = "namedElements"))
        include(project(":implementations:${impl}"))
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
    dependsOn(tasks.remapJar)
    from(tasks.remapJar)
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
