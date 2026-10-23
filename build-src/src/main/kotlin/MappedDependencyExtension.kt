import org.gradle.api.artifacts.ExternalModuleDependency
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

abstract class MappedDependencyExtension(
    private val project : Project
) {
    operator fun invoke(mavenServer : String, dependency : Provider<MinimalExternalModuleDependency>, pathFilter : String) : Provider<ExternalModuleDependency> {
        return project.provider {
            val dep = dependency.get()
            val provider = MappedDependencyProvider(mavenServer, dependency, pathFilter, "${project.rootDir}/lib", project.gradle.getStartParameter().isRefreshDependencies())
            provider.get()
            project.getDependencyFactory().create("local.maven", "${dep.getName()}-api", dep.getVersion()).setChanging(provider.binaryChanged)
        }
    }
}
