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

import com.techshroom.inciseblue.InciseBlueExtension
import com.techshroom.inciseblue.InciseBluePlugin
import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.gradlerio.frc.FRCJavaArtifact
import edu.wpi.first.gradlerio.frc.RoboRIO
import edu.wpi.first.gradlerio.wpi.WPIPlugin
import edu.wpi.first.toolchain.NativePlatforms
import jaci.gradle.deploy.DeployExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.getPlugin

internal val Project.rioExt
    get() = the<GradleRioReduxExtension>()

class GradleRioRedux : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            extensions.create<GradleRioReduxExtension>("gradleRioRedux", project)
            applyPlugins()

            afterEvaluate {
                rioExt.validate()
                unpackVendorDependencies()
                mainSetup()
            }
        }
    }

    private fun Project.applyPlugins() {
        apply<JavaPlugin>()
        apply<InciseBluePlugin>()
        configure<InciseBlueExtension> {
            if (rootProject.file("HEADER.txt").exists()) {
                license()
            }
            util {
                setJavaVersion(JavaVersion.VERSION_11)
            }
            ide()
        }

        apply<GradleRIOPlugin>()
    }

    private fun Project.unpackVendorDependencies() {
        val rioExt = rioExt
        val jsonDepDir = rioExt.jsonDependencyDirectory.asFile
        val pullJsonDependency = PullJsonDependency(
                rioExt.jsonCacheDirectory.asFile.toPath(),
                jsonDepDir.toPath(),
                !gradle.startParameter.isOffline
        )
        rioExt.jsonDependencies.forEach {
            pullJsonDependency.downloadDependencyIfNeeded(it)
        }
        wpi.deps.vendor.loadFrom(jsonDepDir)
    }

    private fun Project.mainSetup() {
        setupDeploy()
        setupDependencies()
        setupFatJar()
        // need to run this again:
        plugins.getPlugin(WPIPlugin::class).addMavenRepositories(project, wpi)
    }

    private fun Project.setupDeploy() {
        configure<DeployExtension> {
            targets {
                target<RoboRIO>("roboRio") {
                    team = rioExt.teamNumber
                }
            }

            artifacts {
                artifact<FRCJavaArtifact>("frcJava") {
                    targets.add("roboRio")
                    debug = frc.getDebugOrDefault(false)
                }
            }
        }
    }

    private fun Project.setupDependencies() {
        dependencies {
            (wpi.deps.vendor.java() + wpi.deps.wpilib()).forEach {
                "implementation"(it)
            }
            wpi.deps.vendor.jni(NativePlatforms.roborio).forEach {
                "nativeZip"(it)
            }
            wpi.deps.vendor.jni(NativePlatforms.desktop).forEach {
                "nativeDesktopZip"(it)
            }
            "testImplementation"("junit:junit:4.12")
        }
    }

    private fun Project.setupFatJar() {
        tasks.named<Jar>("jar") {
            from(delegateClosureOf<Any> {
                configurations.getByName("compile").map {
                    return@map when {
                        it.isDirectory -> it
                        else -> zipTree(it)
                    }
                }
            })

            manifest {
                attributes("Main-Class" to rioExt.mainClass)
            }
        }
    }
}