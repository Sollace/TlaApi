plugins {
    id("fabric-loom")
}

if (project != rootProject) {
    // Root project configures reckon (only root project can do that)
    version = rootProject.version
}

group = rootProject.group

val minecraftVersion = rootProject.property("minecraft_version") as String
val minecraftVersionRange = rootProject.property("minecraft_version_range") as String
val loaderVersion = rootProject.property("loader_version") as String
val fabricVersion = rootProject.property("fabric_version") as String

val neoforgeVersion = rootProject.property("neoforge_version") as String

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
}

tasks.processResources  {
    inputs.property("version", project.version.toString())
    filesMatching("fabric.mod.json") {
        expand(
          "version" to project.version.toString(),
          "loader_version" to loaderVersion,
          "fabric_version" to fabricVersion,
          "minecraft_version" to minecraftVersionRange
        )
    }
    filesMatching("neoforge.mod.toml") {
        expand(
          "version" to project.version.toString(),
          "neo_version" to "[${neoforgeVersion},)", // >=neoforgeVersion
          "minecraft_version" to convertToMavenVersionRange(minecraftVersionRange)
        )
    }
}

fun convertToMavenVersionRange(version : String) : String {
    val symbolLookup = mapOf(
        "<" to ")", "<=" to "]",
        ">" to "(", ">=" to "["
    )
    val symbolReg = "([<>]=?)(.+)".toRegex()
    return version.split(" ")
        .mapIndexed { index : Int, ver : String -> ver.split(symbolReg).filter{ it.length > 0 } }
        .map { if (it.get(0).contains("<")) it.reversed() else it }
        .map { segment : List<String> ->
            segment.map { symbolLookup.getOrDefault(it, it) }
        }
        .map { it.joinToString(separator = "") }
        .joinToString(separator = ",")
}
