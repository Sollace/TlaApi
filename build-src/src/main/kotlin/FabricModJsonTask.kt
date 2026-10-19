
import com.google.gson.JsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import com.google.gson.internal.Streams;

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path as nioPath

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty
import org.gradle.api.file.RegularFileProperty

abstract class FabricModJsonTask : DefaultTask() {
    @get:Input
    abstract val inputFile: Property<File>

    @get:Input
    abstract val modid: Property<String>
    
    @get:Input
    abstract val displayname: Property<String>

    @get:Input
    abstract val modDescription: Property<String>

    @get:Input
    abstract val entrypoints: MapProperty<String, String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun action() {
        Files.newBufferedWriter(outputFile.getAsFile().get().toPath()).use { writer ->
            Files.newBufferedReader(inputFile.get().toPath(), StandardCharsets.UTF_8).use { reader ->
                var json = JsonParser.parseReader(reader).getAsJsonObject()
                val entryPointsJson = JsonObject()
                entrypoints.get().forEach { id, type ->
                    var typesJson = JsonArray()
                    typesJson.add(type)
                    entryPointsJson.add(id, typesJson)
                }
                json.add("entrypoints", entryPointsJson)
                json.addProperty("id", modid.get())
                json.addProperty("name", displayname.get())
                json.addProperty("description", modDescription.get())

                val s = JsonWriter(writer)
                s.setIndent("    ")
                Streams.write(json, s)
                writer.flush()
            }
        }
    }
}
