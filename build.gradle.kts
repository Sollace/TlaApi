import org.gradle.kotlin.dsl.support.uppercaseFirstChar

val minecraftVersion = property("minecraft_version") as String
val yarnMappings = property("yarn_mappings") as String
val loaderVersion = property("loader_version") as String
val fabricVersion = property("fabric_version") as String
val extLibs = libs

plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
    id("org.ajoberstar.reckon") version "0.13.1"
}
apply(plugin = "fabric-loom")

version = "${property("mod_version")}+${minecraftVersion}"
group = "io.github.mattidragon"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

repositories {
    flatDir({ dirs("lib") })
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.blamejared.com/")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    minecraft(libs.minecraft)
    mappings(libs.fabric.yarn.withClassifier("v2"))
}

subprojects {
    version = rootProject.version
    group = rootProject.group
    apply(plugin = "fabric-loom")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withSourcesJar()
    }
    
    repositories {
        flatDir({ dirs("lib") })
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.blamejared.com/")
        maven("https://api.modrinth.com/maven")
    }

    

    dependencies {
        fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

        minecraft(rootProject.libs.minecraft)
        mappings(rootProject.libs.fabric.yarn.withClassifier("v2"))
        modImplementation(rootProject.libs.fabric.loader)
        modImplementation(rootProject.libs.fabric.api)
        implementation(rootProject.libs.devlogin.withClassifier("fabric"))
    }

    configurations.all {
        resolutionStrategy.force(rootProject.libs.fabric.loader)
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
}
/*
tasks.jar {
    from(project(":core"))
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier = "api"
    dependsOn(tasks.jar)
    from(tasks.jar)
    include("io/github.mattidragon/tlaapi/api/**")
}

tasks.build {
    dependsOn(apiJar)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}"}
    }
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
*/


