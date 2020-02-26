package org.rivierarobotics.gradlerioredux.tasks

import org.gradle.api.file.SourceDirectorySet

/**
 * Represents the PathWeaver sources on a [org.gradle.api.tasks.SourceSet].
 */
open class PathWeaverSourceSetExtension {

    lateinit var pathWeaver: SourceDirectorySet
        internal set

}
