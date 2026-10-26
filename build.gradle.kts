plugins {
    id("maven-publish")
    id("org.relativitymc.neo-loom-remap") version "1.17-SNAPSHOT" apply false
    //id("com.modrinth.minotaur") version "2.+"
    id("org.ajoberstar.reckon") version "0.13.1"
    id("minecraft-versions-range")
}

group = "io.github.mattidragon"

reckon {
    scopeFromProp()
    stageFromProp("beta", "rc", "final", "mc" /*legacy tags. Do not use*/)
}

subprojects {
    repositories {
        flatDir({ dirs(File(rootDir, "lib")) })

        maven("https://api.modrinth.com/maven")
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.blamejared.com/")
    }
}
/*
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
}*/
