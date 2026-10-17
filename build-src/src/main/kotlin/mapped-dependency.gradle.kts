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

abstract class MappedDependencyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.<Mapped>create("mapped")
    }
}

abstract class Mapped {
    fun dependency(mavenServer : String, artefactName : Provider<MinimalExternalModuleDependency>, pathFilter : Predicate<String>) {
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
}