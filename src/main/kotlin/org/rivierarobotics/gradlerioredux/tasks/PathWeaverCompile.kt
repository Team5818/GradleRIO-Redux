/*
 * This file is part of GradleRIO-Redux, licensed under the GNU General Public License (GPLv3).
 *
 * Copyright (c) Riviera Robotics <https://github.com/Team5818>
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.rivierarobotics.gradlerioredux.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
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
