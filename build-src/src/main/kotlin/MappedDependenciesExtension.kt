import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.Project

// import net.fabricmc.loom.task.GenerateSourcesTask

import java.io.File
import java.util.function.Predicate

abstract class MappedDependenciesExtension(
    private val project : Project
) {
    operator fun invoke(mavenServer : String, dependency : Provider<MinimalExternalModuleDependency>, pathFilter : String) : Provider<FileCollectionDependency> {
        val dep = dependency.get()
        val depName = (
              dep.getGroup().replaceFirstChar{ it.titlecase() }
            + dep.getName().replaceFirstChar{ it.titlecase() }
        ).replace("[\\./\\\\:<>\"\\?\\*\\|]".toRegex(), "_")

        val provider = MappedDependencyProvider(mavenServer, dependency, pathFilter, "${project.rootDir}/lib", false)
        val downloadTask = project.tasks.register("downloadFilteredDependency${depName}", DownloadFilteredDependencyTask::class.java) {
            outputFilesDir.set("${project.rootDir}/lib")
            this.mavenServer.set(mavenServer)
            this.dependency.set(dependency)
            this.pathFilter.set(pathFilter)
            this.outputFiles.set(project.provider {
              project.files(listOf(
                  provider.getOutputFileLocation(dep, ""),
                  provider.getOutputFileLocation(dep, "-sources")
              ).filter{ it.exists() })
            })
            group = "TLA"
            description = "Generate API jars for dependencies"
        }

        project.tasks.getByName("compileJava").dependsOn(downloadTask)

        /*val genDependencySourcesWithCfr = project.tasks.register("genDependencySourcesWithCfr", GenerateSourcesTask::class.java) {
            description = "Generate Sources for sourceless dependency jar"
            group = "TLA"
        }
        */

        return downloadTask.flatMap{ it.outputFiles }.map{ project.getDependencyFactory().create(it) }
    }
}
