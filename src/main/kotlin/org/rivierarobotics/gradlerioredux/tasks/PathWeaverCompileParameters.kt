package org.rivierarobotics.gradlerioredux.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkParameters

interface PathWeaverCompileParameters : WorkParameters {
    val projectDirectory: DirectoryProperty
    val sourceFile: RegularFileProperty
    val outputDirectory: DirectoryProperty
}
