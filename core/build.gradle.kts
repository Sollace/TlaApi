plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
}

version = rootProject.version
group = rootProject.group

val minecraftVersion = rootProject.property("minecraft_version") as String
val yarnMappings = rootProject.property("yarn_mappings") as String
val loaderVersion = rootProject.property("loader_version") as String
val fabricVersion = rootProject.property("fabric_version") as String

val SourceSetContainer.client: SourceSet
    get() = this["client"]
val SourceSetContainer.main: SourceSet
    get() = this["main"]
val SourceSetContainer.test: SourceSet
    get() = this["test"]

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
            setSrcDirs(listOf("../core/src/main/java"))
        }
        resources {
            setSrcDirs(listOf("../core/src/main/resources"))
        }
    }

    named("client") {
        java {
            setSrcDirs(listOf("../core/src/client/java"))
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
    createRemapConfigurations(sourceSets.test)

    mods {
        create("test") {
            sourceSet(sourceSets.test)
        }
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
