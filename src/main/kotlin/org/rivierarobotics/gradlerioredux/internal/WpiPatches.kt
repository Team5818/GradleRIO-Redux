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

/**
 * Patch up some WPI plugins for Kotlin usage.
 */
package org.rivierarobotics.gradlerioredux.internal

import edu.wpi.first.gradlerio.deploy.FRCExtension
import edu.wpi.first.gradlerio.wpi.WPIExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.rivierarobotics.gradlerioredux.DownloadInfo
import java.net.URL
import java.nio.file.Files

fun Project.wpiVendorDownloadInfo() =
    wpi.vendor.dependencySet
        .map { it.dependency }
        .filterNot { it.jsonUrl.isNullOrEmpty() }
        .map {
            // Note: this isn't strictly the same as WPI but it's good enough for me
            val file = project.file("vendordeps").toPath().resolve(it.fileName)
            require(Files.exists(file)) {
                "Vendor dep ${it.name} expects to be a file named ${it.fileName}," +
                        " but that file does not exist!"
            }
            DownloadInfo(file, URL(it.jsonUrl))
        }


val Project.frc
    get() = the<FRCExtension>()
val Project.wpi
    get() = the<WPIExtension>()
