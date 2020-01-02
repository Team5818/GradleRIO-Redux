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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.KotlinClosure0
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

internal val Project.rioExt
    get() = the<GradleRioReduxExtension>()

class GradleRioRedux : Plugin<Project> {
    lateinit var mainGeneration: TaskProvider<RobotMainGeneration>
    lateinit var mainJavaCompile: TaskProvider<JavaCompile>

    override fun apply(project: Project) {
        project.run {
            extensions.create<GradleRioReduxExtension>("gradleRioRedux", project)
            applyPlugins()
            setupMainGenerationTask()

            afterEvaluate {
                rioExt.validate()
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
                addRepositories = false
            }
            ide()
        }

        apply<GradleRIOPlugin>()
    }

    private fun Project.mainSetup() {
        setupDeploy()
        setupDependencies()
        setupFatJar()
        // need to run this again:
        plugins.getPlugin(WPIPlugin::class).addMavenRepositories(project, wpi)
    }

    private fun Project.setupMainGenerationTask() {
        mainGeneration = tasks.register<RobotMainGeneration>("robotMainGeneration") {
            this.robotClass.set(rioExt.robotClassProperty)
        }
        mainJavaCompile = tasks.register<JavaCompile>("compileJava${mainGeneration.name.capitalize()}") {
            val compileJava = tasks.getByName("compileJava")
            val mgen = mainGeneration.get()

            dependsOn(compileJava, mgen)

            setSource(mgen.outputFile)
            setDestinationDir(project.layout.buildDirectory.dir("${mgen.name}/classes").map { it.asFile })
            classpath = project.files(compileJava.outputs, configurations["compileClasspath"])
        }
    }

    private fun Project.setupDeploy() {
        configure<DeployExtension> {
            targetsKt {
                targetKt<RoboRIO>("roboRio") {
                    team = rioExt.teamNumber
                }
            }

            artifactsKt {
                artifactKt<FRCJavaArtifact>("frcJava") {
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
            from(KotlinClosure0(function = {
                return@KotlinClosure0 configurations["runtimeClasspath"].map {
                    return@map when {
                        it.isDirectory -> it
                        else -> zipTree(it)
                    }
                }
            }), mainJavaCompile)

            manifest {
                attributes("Main-Class" to mainGeneration.get().mainClassFqn.get())
            }
        }
    }
}
