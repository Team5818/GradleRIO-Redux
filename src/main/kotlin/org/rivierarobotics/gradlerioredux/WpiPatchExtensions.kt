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
 * Patch up some of the WPI plugins for Kotlin usage.
 */
package org.rivierarobotics.gradlerioredux

import edu.wpi.first.gradlerio.frc.FRCExtension
import edu.wpi.first.gradlerio.wpi.WPIExtension
import edu.wpi.first.gradlerio.wpi.dependencies.WPIDepsExtension
import edu.wpi.first.gradlerio.wpi.dependencies.WPIVendorDepsExtension
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.Artifact
import jaci.gradle.deploy.artifact.ArtifactsExtension
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.TargetsExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.nativeplatform.platform.NativePlatform
import java.net.URL
import java.nio.file.Files

fun DeployExtension.targetsKt(action: TargetsExtension.() -> Unit) {
    targets.action()
}

inline fun <reified T : RemoteTarget> TargetsExtension.targetKt(name: String, config: Action<T>) {
    target(name, T::class.java, config)
}

fun DeployExtension.artifactsKt(action: ArtifactsExtension.() -> Unit) {
    artifacts.action()
}

inline fun <reified T : Artifact> ArtifactsExtension.artifactKt(name: String, config: Action<T>) {
    artifact(name, T::class.java, config)
}

fun WPIDepsExtension.allJavaDeps() = vendor.java() + wpilib()

fun WPIDepsExtension.allJniDeps(platform: String): List<String> {
    return vendor.jni(platform) + wpilibJni(platform)
}

val WPIVendorDepsExtension.downloadInfo
    get() = dependencies
        .filterNot { it.jsonUrl.isNullOrEmpty() }
        .map {
            val file = vendorFolder.toPath().resolve(it.fileName)
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
