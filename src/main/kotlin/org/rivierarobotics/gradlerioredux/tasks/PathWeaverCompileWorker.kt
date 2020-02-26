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
