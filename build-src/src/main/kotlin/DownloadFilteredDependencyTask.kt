import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

import kotlin.text.Regex

import java.io.IOException
import java.io.File
import java.net.URI
import java.nio.file.Files

import java.util.function.Predicate
import java.util.function.Supplier
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

abstract class DownloadFilteredDependencyTask : DefaultTask() {
    @get:Input
    abstract val mavenServer : Property<String>

    @get:Input
    abstract val dependency : Property<Provider<MinimalExternalModuleDependency>>

    @get:Input
    abstract val pathFilter : Property<String>

    @get:Input
    abstract val outputFilesDir : Property<String>

    @get:OutputFiles
    abstract val outputFiles: Property<FileCollection>

    fun getJarUri(dep : MinimalExternalModuleDependency, classifier : String) : URI {
        return URI.create("${mavenServer.get()}/${dep.getGroup().replace(".", "/")}/${dep.getName()}/${dep.getVersion()}/${dep.getName()}-${dep.getVersion()}${classifier}.jar")
    }

    fun getBaseName(dep : MinimalExternalModuleDependency, classifier : String) : String {
        return "${dep.getName()}-api-${dep.getVersion()}${classifier}.jar"
    }
    
    fun getOutputFileLocation(dep : MinimalExternalModuleDependency, classifier : String) : File {
        return File("${outputFilesDir.get()}/${getBaseName(dep, classifier)}")
    }

    @TaskAction
    fun action() {
        MappedDependencyProvider(mavenServer.get(), dependency.get(), pathFilter.get(), outputFilesDir.get()).get()
    }
}