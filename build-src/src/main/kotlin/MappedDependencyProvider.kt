import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.Project

import java.io.IOException
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path as nioPath
import java.util.function.Predicate
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class MappedDependencyProvider(
    private val mavenServer : String,
    private val dependency : Provider<MinimalExternalModuleDependency>,
    private val pathFilter : String,
    private val outputFilesDir : String,
    private val force : Boolean
) {
    fun get() : List<File> {
        val outputs = mutableListOf<File>()
        val dep = dependency.get()
        downloadDep(dep, outputs)
        downloadSource(dep, outputs)

        return outputs
    }

    var binaryChanged : Boolean = false

    fun getJarUri(dep : MinimalExternalModuleDependency, classifier : String) : URI {
        return URI.create("${mavenServer}/${dep.getGroup().replace(".", "/")}/${dep.getName()}/${dep.getVersion()}/${dep.getName()}-${dep.getVersion()}${classifier}.jar")
    }

    fun getBaseName(dep : MinimalExternalModuleDependency, classifier : String) : String {
        return "${dep.getName()}-api-${dep.getVersion()}${classifier}.jar"
    }

    fun getOutputFileLocation(dep : MinimalExternalModuleDependency, classifier : String) : File {
        return File("${outputFilesDir}/${getBaseName(dep, classifier)}")
    }

    protected fun downloadDep(dep : MinimalExternalModuleDependency, outputs : MutableList<File>) {
        val outputLocation = getOutputFileLocation(dep, "")
        if (force && Files.exists(outputLocation.toPath())) {
            Files.delete(outputLocation.toPath())
        }
        if (!Files.exists(outputLocation.toPath())) {
            binaryChanged = true
            attemptDepDownload(0, 3, dep, getJarUri(dep, ""), outputLocation)
        }
        outputs.add(outputLocation)
    }

    protected fun attemptDepDownload(
            attempt : Int,
            totalAttempts : Int,
            dep : MinimalExternalModuleDependency,
            uri : URI,
            outputLocation : File
        ) {
        System.out.println("Download dependency file (attempt ${attempt}/${totalAttempts}: ${outputLocation}")
        try {
            ZipInputStream(uri.toURL().openStream()).use { inZipStream ->
                ZipOutputStream(Files.newOutputStream(outputLocation.toPath())).use { outZipStream ->
                    this.rewriteZip(dep, pathFilter.toRegex(), inZipStream, outZipStream)
                }
            }
        } catch (e : IOException) {
            if (attempt < totalAttempts) {
                attemptDepDownload(attempt + 1, totalAttempts, dep, uri, outputLocation)
            } else {
                throw e
            }
        }
    }

    protected fun downloadSource(dep : MinimalExternalModuleDependency, outputs : MutableList<File>) {
        val sourcesOutputLocation = getOutputFileLocation(dep, "-sources")
        if (force && Files.exists(sourcesOutputLocation.toPath())) {
            Files.delete(sourcesOutputLocation.toPath());
        }
        if (!Files.exists(sourcesOutputLocation.toPath())) {
            System.out.println("Download dependency file: ${sourcesOutputLocation}")
            try {
                getJarUri(dep, "-sources").toURL().openStream().use { inSourceStream ->
                    Files.newOutputStream(sourcesOutputLocation.toPath()).use { outFileStream ->
                        inSourceStream.transferTo(outFileStream)
                    }
                }
                binaryChanged = true
                outputs.add(sourcesOutputLocation)
            } catch (e : IOException) {
                System.out.println(e)
            }
        }
    }

    private fun rewriteZip(dep : MinimalExternalModuleDependency, pathFilter : Regex, inZipStream : ZipInputStream, outZipStream : ZipOutputStream) {
        val buffer = ByteArray(16384)
        while (true) {
            val entry = inZipStream.getNextEntry()
            if (entry == null) {
                break;
            }
            if (entry.getName().contains("fabric.mod.json")) {
                writeFmj(dep, entry, outZipStream)
            } else if (entry.getName().contains("META-INF/MANIFEST") || pathFilter.matches(entry.getName())) {
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

    private fun writeFmj(dep : MinimalExternalModuleDependency, entry : ZipEntry, outZipStream : ZipOutputStream) {
        val jsonBytes = ("{ \"id\": \"${dep.getName().split("-")[0]}-api\", \"version\": \"${dep.getVersion()}\", \"schemaVersion\": 1 }").toByteArray()
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
    }
}
