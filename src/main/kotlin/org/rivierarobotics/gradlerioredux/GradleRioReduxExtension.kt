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
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.gradle.kotlin.dsl.setValue
import java.nio.file.Files

open class GradleRioReduxExtension(project: Project) {
    val mainClassProperty: Property<String> = project.objects.property()
    var mainClass: String by mainClassProperty

    val teamNumberProperty: Property<Int> = project.objects.property()
    var teamNumber: Int by teamNumberProperty

    var addCtre: Boolean = false

    val extraJsonDependenciesProperty: SetProperty<String> = project.objects.setProperty<String>().also {
        it.set(listOf())
    }
    var extraJsonDependencies: Set<String>
        get() = extraJsonDependenciesProperty.get()
        set(value) = extraJsonDependenciesProperty.set(value)

    val jsonDependencyDirectoryProperty: DirectoryProperty = project.objects.directoryProperty().also {
        it.set(project.layout.buildDirectory.dir("gradlerioredux-jsondeps"))
    }
    var jsonDependencyDirectory: Directory by jsonDependencyDirectoryProperty

    val jsonCacheDirectoryProperty: DirectoryProperty = project.objects.directoryProperty().also {
        it.set(project.gradle.gradleUserHomeDir.resolve("caches/gradlerioredux-jsoncache"))
    }
    var jsonCacheDirectory: Directory by jsonCacheDirectoryProperty

    val jsonDependencies: Set<String>
        get() = sequence {
            if (addCtre) {
                yield("http://devsite.ctr-electronics.com/maven/release/com/ctre/phoenix/Phoenix-latest.json")
            }
            yieldAll(extraJsonDependencies)
        }.toSet()

    fun validate() {
        if (!mainClassProperty.isPresent) {
            throw IllegalStateException("Missing value for mainClass!")
        }
        if (!teamNumberProperty.isPresent) {
            throw IllegalStateException("Missing value for teamNumber!")
        }
        Files.createDirectories(jsonCacheDirectory.asFile.toPath())
        Files.createDirectories(jsonDependencyDirectory.asFile.toPath())
    }
}