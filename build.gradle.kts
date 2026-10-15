import org.gradle.kotlin.dsl.support.uppercaseFirstChar

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path as nioPath;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
    id("org.ajoberstar.reckon") version "0.13.1"
}

val minecraftVersion = property("minecraft_version") as String
val yarnMappings = property("yarn_mappings") as String
val loaderVersion = property("loader_version") as String
val fabricVersion = property("fabric_version") as String

val recipeMods = (property("recipe_mod_modules") as String).split(" ")
val enabledRecipeMods = (property("enabled_recipe_mod_modules") as String).split(" ")

version = "${property("mod_version")}+${minecraftVersion}"
group = "io.github.mattidragon"

repositories {
    flatDir({ dirs("lib") })
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.blamejared.com/")
    maven("https://api.modrinth.com/maven")
}

loom.splitEnvironmentSourceSets()

val SourceSetContainer.client: SourceSet
    get() = this["client"]
val SourceSetContainer.main: SourceSet
    get() = this["main"]

loom {
    mods.register("tla-api") {
        sourceSet(sourceSets.main)
        sourceSet(sourceSets.client)
    }

    runs["client"].ideConfigGenerated(false)
}

sourceSets.create("testmod") {
    compileClasspath += sourceSets.main.compileClasspath + sourceSets.main.output
    runtimeClasspath += sourceSets.main.runtimeClasspath + sourceSets.main.output
    compileClasspath += sourceSets.client.compileClasspath + sourceSets.client.output
    runtimeClasspath += sourceSets.client.runtimeClasspath + sourceSets.client.output
}

val SourceSetContainer.testmod: SourceSet
    get() = this["testmod"]

loom {
    createRemapConfigurations(sourceSets.testmod)
    mods.create("tlaapi_testmod").sourceSet(sourceSets.testmod)
}

recipeMods.forEach { name ->
    val compilePaths = sourceSets.main.compileClasspath +
            sourceSets.main.output +
            sourceSets.client.compileClasspath +
            sourceSets.client.output
    val runtimePaths = sourceSets.main.runtimeClasspath +
            sourceSets.main.output +
            sourceSets.client.runtimeClasspath +
            sourceSets.client.output +
            sourceSets.testmod.output

    val sourceSet = sourceSets.create(name) {
        compileClasspath += compilePaths
        runtimeClasspath += runtimePaths

        resources.setSrcDirs(listOf("implSrc/$name/resources"))
        java.setSrcDirs(listOf("implSrc/$name/java"))
    }

    tasks[sourceSet.classesTaskName].dependsOn(sourceSets.testmod.classesTaskName)

    loom {
        runs.register("clientWith${name.uppercaseFirstChar()}") {
            inherit(runs["client"])
            ideConfigGenerated(true)
            configName = "Minecraft Client with ${name.uppercase()}"
            runDir("run/${name}")
            source(name)
        }

        createRemapConfigurations(sourceSet)
        mods["tla-api"].sourceSet(sourceSet)
    }

    tasks.jar {
        from(sourceSet.output)
        dependsOn(sourceSet.classesTaskName)
    }

    tasks.remapJar {
        classpath.from(configurations.getByName(sourceSet.compileClasspathConfigurationName))
    }
}

dependencies {
    fun Provider<MinimalExternalModuleDependency>.withClassifier(classifier: String) = variantOf(this) { this.classifier(classifier) }

    minecraft(libs.minecraft)
    mappings(libs.fabric.yarn.withClassifier("v2"))
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    implementation(libs.devlogin.withClassifier("fabric"))

    add("modReiCompileOnly", libs.rei.api)
    add("modReiCompileOnly", libs.rei.plugin.default)
    // For some reason arch isn't a transitive dependency of rei-api, so we need to manually add it to use a few classes
    add("modReiCompileOnly", libs.rei.architectury)
    add("modReiCompileOnly", libs.rei.config)

    if (enabledRecipeMods.indexOf("rei") > -1) {
      add("modReiRuntimeOnly", libs.rei.all)
    }

    add("modEmiCompileOnly", libs.emi.withClassifier("api"))
    if (enabledRecipeMods.indexOf("emi") > -1) {
      add("modEmiRuntimeOnly", libs.emi)
    }

    // Because of issues with the remapping of the jei api, we have to depend on the fat jar instead (https://github.com/mezz/JustEnoughItems/issues/2891)
    mappedDependency("https://maven.blamejared.com", libs.jei.fabric) { path -> path.contains("/api/") || path.contains("DrawableAnimated") }
    add("modJeiCompileOnly", libs.jei.api.fabric)
    if (enabledRecipeMods.indexOf("jei") > -1) {
      add("modJeiRuntimeOnly", libs.jei.fabric)
    }

    mappedDependency("https://api.modrinth.com/maven", libs.eiv.fabric) { path -> path.contains("/api/") }
    add("modEivCompileOnly", libs.eiv.api.fabric)
    if (enabledRecipeMods.indexOf("eiv") > -1) {
      add("modEivRuntimeOnly", libs.eiv.fabric)
    }
}

configurations.all {
    resolutionStrategy.force(libs.fabric.loader)
}

tasks.processResources  {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
        expand("loader_versions" to loaderVersion)
        expand("fabric_version" to fabricVersion)
        expand("minecraft_version" to minecraftVersion)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
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


fun mappedDependency(mavenServer : String, artefactName : Provider<MinimalExternalModuleDependency>, pathFilter : Predicate<String>) {
    val downloadArtefact = artefactName.get().toString().split(":")
    val outputLocation = nioPath.of("lib/${downloadArtefact[1]}-api-${downloadArtefact[2]}.jar") as java.nio.file.Path;

    if (!Files.exists(outputLocation)) {
      val downloadUri = URI.create("${mavenServer}/${downloadArtefact[0].replace(".", "/")}/${downloadArtefact[1]}/${downloadArtefact[2]}/${downloadArtefact[1]}-${downloadArtefact[2]}.jar")
      ZipInputStream(downloadUri.toURL().openStream()).use { inZipStream ->
        ZipOutputStream(Files.newOutputStream(outputLocation)).use { outZipStream ->
            val buffer = ByteArray(16384)
            while (true) {
              val entry = inZipStream.getNextEntry()
              if (entry == null) {
                  break;
              }
              if (entry.getName().contains("fabric.mod.json")) {
                val jsonBytes = ("{ \"id\": \"${downloadArtefact[1]}-api\", \"version\": \"${downloadArtefact[2]}\", \"schemaVersion\": 1 }").toByteArray()
                val newEntry = ZipEntry(entry.getName())
                val crc = CRC32()
                crc.update(jsonBytes, 0, jsonBytes.size);
                newEntry.setSize(jsonBytes.size.toLong())
                newEntry.setCrc(crc.getValue())
                newEntry.setTime(entry.getTime())
                newEntry.setMethod(0)
                outZipStream.putNextEntry(newEntry)
                outZipStream.write(jsonBytes)
                outZipStream.closeEntry()
              } else if (entry.getName().contains("META-INF/MANIFEST") || pathFilter.test(entry.getName())) {
                outZipStream.putNextEntry(entry)
                while (inZipStream.available() > 0) {
                  val bytesRead = inZipStream.read(buffer)
                  if (bytesRead > 0) {
                    outZipStream.write(buffer, 0, bytesRead)
                  }
                }
                outZipStream.closeEntry()
              } else if (entry.getCompressedSize() > 0) {
                inZipStream.skip(entry.getCompressedSize());
              }
              inZipStream.closeEntry()
            }
            outZipStream.flush();
         }
      }
   }

   val sourcesOutputLocation = nioPath.of("lib/${downloadArtefact[1]}-api-${downloadArtefact[2]}-sources.jar") as java.nio.file.Path;
   if (!Files.exists(sourcesOutputLocation)) {
      val sourcesDownloadUri = URI.create("${mavenServer}/${downloadArtefact[0].replace(".", "/")}/${downloadArtefact[1]}/${downloadArtefact[2]}/${downloadArtefact[1]}-${downloadArtefact[2]}-sources.jar")
      try {
        sourcesDownloadUri.toURL().openStream().use { inSourceStream ->
          Files.newOutputStream(sourcesOutputLocation).use { outFileStream ->
            inSourceStream.transferTo(outFileStream)
          }
        }
      } catch (e : IOException) {
        System.out.println(e)
      }
    }
}
