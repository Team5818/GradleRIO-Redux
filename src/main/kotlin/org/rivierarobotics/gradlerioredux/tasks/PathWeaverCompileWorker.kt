package org.rivierarobotics.gradlerioredux.tasks

import edu.wpi.first.pathweaver.PathIOUtil
import edu.wpi.first.pathweaver.ProjectPreferences
import org.gradle.workers.WorkAction

abstract class PathWeaverCompileWorker : WorkAction<PathWeaverCompileParameters> {
    override fun execute() {
        val project = parameters.projectDirectory.get().asFile.toPath()
        val source = parameters.sourceFile.get().asFile.toPath()
        val output = parameters.outputDirectory.get().asFile.toPath()

        ProjectPreferences.getInstance(project.toRealPath().toString())

        val path = PathIOUtil.importPath(source.parent.toString(), source.fileName.toString())
        val pathNameFile = output.resolve(path.pathNameNoExtension)
        check(path.spline.writeToFile(pathNameFile)) { "Unable to write spline from $source to $output" }
    }
}
