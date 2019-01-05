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
import jaci.gradle.deploy.DeployExtension
import jaci.gradle.deploy.artifact.Artifact
import jaci.gradle.deploy.artifact.ArtifactsExtension
import jaci.gradle.deploy.target.RemoteTarget
import jaci.gradle.deploy.target.TargetsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.the

fun DeployExtension.targets(action: TargetsExtension.() -> Unit) {
    targets.action()
}

inline fun <reified T : RemoteTarget> TargetsExtension.target(name: String, noinline config: T.() -> Unit) {
    target(name, T::class.java, delegateClosureOf(config))
}

fun DeployExtension.artifacts(action: ArtifactsExtension.() -> Unit) {
    artifacts.action()
}

inline fun <reified T : Artifact> ArtifactsExtension.artifact(name: String, noinline config: T.() -> Unit) {
    artifact(name, T::class.java, delegateClosureOf(config))
}

val Project.frc
    get() = the<FRCExtension>()
val Project.wpi
    get() = the<WPIExtension>()
