package org.rivierarobotics.gradlerioredux.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import org.rivierarobotics.gradlerioredux.PATH_WEAVER_CONFIGURATION
import javax.inject.Inject

/**
 * Compiles PathWeaver CSVs to spline json files.
 */
open class PathWeaverCompile @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @InputDirectory
    val projectDirectoryProperty = project.objects.directoryProperty()

    @OutputDirectory
    val outputDirectoryProperty = project.objects.directoryProperty()

    @get:Internal
    var outputDirectory: Directory by outputDirectoryProperty

    // we need info from SourceDirectorySet in particular
    // but people shouldn't set it, it's managed by the plugin
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var sourceFiles: SourceDirectorySet
        internal set

    @TaskAction
    fun compile() {
        val workQueue = workerExecutor.processIsolation {
            this.classpath.from(project.configurations[PATH_WEAVER_CONFIGURATION])
        }
        for (tree in sourceFiles.srcDirTrees) {
            for (file in project.fileTree(tree.dir).matching(tree.patterns).files) {
                workQueue.submit(PathWeaverCompileWorker::class) {
                    projectDirectory.set(projectDirectoryProperty)
                    sourceFile.set(file)
                    outputDirectory.set(outputDirectoryProperty)
                }
            }
        }
    }
}
