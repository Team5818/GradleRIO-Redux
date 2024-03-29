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

package org.rivierarobotics.gradlerioredux

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*

open class GradleRioReduxExtension(private val project: Project) {
    val robotClassProperty: Property<String> = project.objects.property()
    var robotClass: String by robotClassProperty

    val teamNumberProperty: Property<Int> = project.objects.property()
    var teamNumber: Int by teamNumberProperty

    val pathWeaverProjectProperty = project.objects.directoryProperty()

    val checkstyleVersionProperty: Property<String> = project.objects.property()
    val sevntuVersionProperty: Property<String> = project.objects.property()

    fun applyGradleRioConfiguration() {
        project.plugins.getPlugin(GradleRioRedux::class).applyGradleRioConfiguration(project)
    }
}
