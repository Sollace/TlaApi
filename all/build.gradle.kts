plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
}

val minecraftVersion = rootProject.property("minecraft_version") as String
val yarnMappings = rootProject.property("yarn_mappings") as String
val loaderVersion = rootProject.property("loader_version") as String
val fabricVersion = rootProject.property("fabric_version") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("../core/src"))
        }
        resources {
            setSrcDirs(listOf("../core/src"))
        }
    }

    client {
        java {
            setSrcDirs(listOf("../core/src"))
        }
    }

    test {
        compileClasspath += sourceSets.main.compileClasspath + sourceSets.main.output
        runtimeClasspath += sourceSets.main.runtimeClasspath + sourceSets.main.output
        compileClasspath += sourceSets.client.compileClasspath + sourceSets.client.output
        runtimeClasspath += sourceSets.client.runtimeClasspath + sourceSets.client.output
    }
}

loom {
    splitEnvironmentSourceSets()

    createRemapConfigurations(sourceSets.test)
    mods {
      tlaapi_testmod {
          sourceSet(sourceSets.test)
      }
    }
}

dependencies {
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    minecraft(rootProject.libs.minecraft)
    mappings(rootProject.libs.fabric.yarn.withClassifier("v2"))
    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)

    include(project(":implementations:eiv"))
    include(project(":implementations:emi"))
    include(project(":implementations:jei"))
    include(project(":implementations:rei"))
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


val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier = "api"
    dependsOn(tasks.remapJar)
    from(tasks.remapJar)
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
