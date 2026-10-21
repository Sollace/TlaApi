import dex.plugins.outlet.v2.util.ReleaseType

plugins {
    id("maven-publish")
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("com.modrinth.minotaur") version "2.+"
    id("org.ajoberstar.reckon") version "0.13.1"
    id("io.github.dexman545.outlet") version "1.6.1"
    id("split-sources")
    id("minecraft-mod")
}

group = "io.github.mattidragon"

val enabledRecipeModModules = (property("recipe_mod_modules") as String).split(" ")

reckon {
    scopeFromProp()
    stageFromProp("beta", "rc", "final", "mc" /*legacy tags. Do not use*/)
}

outlet {
    if ("true".equals(property("minecraft_version_allow_snapshot") as String)) {
        allowedReleaseTypes.add(ReleaseType.SNAPSHOT)
    } else {
        allowedReleaseTypes.remove(ReleaseType.SNAPSHOT)
    }
    mcVersionRange = property("minecraft_version_range") as String
}

System.out.println("Outlet MC Versions: ${outlet.mcVersions()}")

subprojects {
    repositories {
        flatDir({ dirs(File(rootDir, "lib")) })

        maven("https://api.modrinth.com/maven")
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.blamejared.com/")
    }
}

dependencies {
    enabledRecipeModModules.forEach { impl ->
        if (impl.length > 0) {
            implementation(project(path = ":implementations:${impl}", configuration = "namedElements"))
            include(project(":implementations:${impl}"))
        }
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

modrinth {
    token.set(System.getenv("MODRINTH_KEY"))
    projectId.set(property("modrinth_project_id") as String)
    //featured = true
    versionNumber.set(version.toString())
    versionName.set("${project.name}-${version}")
    changelog.set("[Changelog](https://github.com/MattiDragon/TlaApi/releases/tag/${version.toString()})")
    loaders.add("fabric")
    uploadFile.set(tasks.remapJar)
    outlet.mcVersions().forEach{ver ->
        gameVersions.add(ver)
    }
    dependencies {
        required.project("P7dR8mSH") // Fabric API
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
