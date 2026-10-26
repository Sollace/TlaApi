
import com.google.gson.JsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import com.google.gson.internal.Streams;

import java.io.File
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path as nioPath

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.file.RegularFileProperty

abstract class TomlFileTask : DefaultTask() {
    @get:Input
    abstract val mainSection: MapProperty<String, String>
    
    @get:Input
    abstract val sections : ListProperty<Pair<String, Map<String, String>>>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun action() {
        Files.newBufferedWriter(outputFile.getAsFile().get().toPath()).use { writer : BufferedWriter ->
            mainSection.get().forEach{ key : String, value : String ->
                writer.write("${key}=\"${value}\"${System.lineSeparator()}")
            }
            sections.get().forEach{ section ->
                writer.write("[[${section.first}]]${System.lineSeparator()}")
                section.second.forEach{ key : String, value : String ->
                    writer.write("    ${key}=\"${value}\"${System.lineSeparator()}")
                }
            }
        }
    }
}
