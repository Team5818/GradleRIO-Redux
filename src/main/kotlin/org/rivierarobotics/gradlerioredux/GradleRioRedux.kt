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

import com.google.common.io.Resources
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
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.dir
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.rivierarobotics.gradlerioredux.tasks.CheckVendorDeps
import org.rivierarobotics.gradlerioredux.tasks.PathWeaverCompile
import org.rivierarobotics.gradlerioredux.tasks.PathWeaverSourceSetExtension
import org.rivierarobotics.gradlerioredux.tasks.UpdateVendorDeps

internal val Project.rioExt
    get() = the<GradleRioReduxExtension>()

private const val TASK_GROUP = "GradleRIO-Redux"
const val PATH_WEAVER_CONFIGURATION = "grrPathWeaver"

class GradleRioRedux : Plugin<Project> {
    lateinit var mainGeneration: TaskProvider<RobotMainGeneration>
    lateinit var mainJavaCompile: TaskProvider<JavaCompile>

    override fun apply(project: Project) {
        project.run {
            extensions.create<GradleRioReduxExtension>("gradleRioRedux", project)
            applyPlugins()
            setupTasks()
            configureCheckstyle()
            setupPathWeaverSourceProcessing()

            afterEvaluate {
                rioExt.validate()
                mainSetup()
            }
        }
    }

    private fun Project.applyPlugins() {
        apply(plugin = "java")
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
        apply(plugin = "checkstyle")
    }

    private fun Project.configureCheckstyle() {
        val checkstyleConfig = tasks.register("extractCheckstyleConfiguration") {
            val output = project.layout.buildDirectory.file("gradlerioredux/checkstyle.xml")
            outputs.file(output)
            doLast {
                output.get().asFile.outputStream().use {
                    Resources.copy(
                        Resources.getResource("org/rivierarobotics/gradlerioredux/checkstyle.xml"),
                        it)
                }
            }
        }
        configure<CheckstyleExtension> {
            config = resources.text.fromFile(checkstyleConfig)
            toolVersion = "8.29"
        }
    }

    private fun Project.mainSetup() {
        setupDeploy()
        setupDependencies()
        setupFatJar()
        // need to run this again:
        plugins.getPlugin(WPIPlugin::class).addMavenRepositories(project, wpi)
    }

    private fun Project.setupTasks() {
        mainGeneration = tasks.register<RobotMainGeneration>("robotMainGeneration") {
            description = "Generates the Main file for the robot."
            group = TASK_GROUP

            this.robotClass.set(rioExt.robotClassProperty)
        }
        mainJavaCompile = tasks.register<JavaCompile>("compile${mainGeneration.name.capitalize()}Java") {
            description = "Compiles the Main file for the robot."
            group = TASK_GROUP

            val compileJava = tasks.getByName("compileJava")
            val mgen = mainGeneration.get()

            dependsOn(compileJava, mgen)

            setSource(mgen.outputFile)
            setDestinationDir(project.layout.buildDirectory.dir("${mgen.name}/classes").map { it.asFile })
            classpath = project.files(compileJava.outputs, configurations["compileClasspath"])
        }
        tasks.register<CheckVendorDeps>("checkVendorDeps") {
            description = "Check the vendor dependency JSON files for updates."
            group = TASK_GROUP
        }
        tasks.register<UpdateVendorDeps>("updateVendorDeps") {
            description = "Update the vendor dependency JSON files."
            group = TASK_GROUP
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
        configurations.create(PATH_WEAVER_CONFIGURATION)
        dependencies {
            wpi.deps.allJavaDeps().forEach {
                "implementation"(it)
            }
            wpi.deps.allJniDeps(NativePlatforms.roborio).forEach {
                "nativeZip"(it)
            }
            wpi.deps.allJniDeps(NativePlatforms.desktop).forEach {
                "nativeDesktopZip"(it)
            }
            "testImplementation"("junit:junit:4.12")

            PATH_WEAVER_CONFIGURATION("edu.wpi.first.wpilib:PathWeaver:${wpi.pathWeaverVersion}:${wpi.toolsClassifier}")
        }
    }

    private fun Project.setupFatJar() {
        tasks.named<Jar>("jar") {
            from(provider {
                configurations["runtimeClasspath"].map {
                    when {
                        it.isDirectory -> it
                        else -> zipTree(it)
                    }
                }
            }, mainJavaCompile)

            manifest {
                attributes("Main-Class" to mainGeneration.get().mainClassFqn.get())
            }
        }
    }

    private fun Project.setupPathWeaverSourceProcessing() {
        convention.getPlugin<JavaPluginConvention>().sourceSets.configureEach {
            val sourceSet = this
            with(extensions) {
                val extension = create<PathWeaverSourceSetExtension>("pathWeaver")
                val sourceDir = project.objects.sourceDirectorySet(
                    "pathWeaver", "${sourceSet.name} PathWeaver source"
                )
                sourceDir.srcDir(project.layout.projectDirectory.dir("src/${sourceSet.name}/pathWeaver"))
                sourceDir.include("**/*")
                sourceDir.destinationDirectory.convention(
                    project.layout.buildDirectory.dir("pathWeaver/${sourceSet.name}")
                )

                extension.pathWeaver = sourceDir
                val task = tasks.register<PathWeaverCompile>(sourceSet.getCompileTaskName("pathWeaver")) {
                    group = TASK_GROUP
                    description = "Compiles ${sourceDir.displayName}"
                    sourceFiles = sourceDir
                    outputDirectoryProperty.convention(sourceDir.destinationDirectory)
                    projectDirectoryProperty.convention(rioExt.pathWeaverProjectProperty)
                }
                extension.pathWeaver.compiledBy(task) { it.outputDirectoryProperty }
                sourceSet.resources.srcDir(sourceDir.destinationDirectory)
                sourceSet.allSource.source(sourceDir)
                sourceSet.output.dir(sourceDir.destinationDirectory, "builtBy" to task)
                project.plugins.withId("idea") {
                    afterEvaluate {
                        configure<IdeaModel> {
                            module.generatedSourceDirs.add(sourceDir.destinationDirectory.asFile.get())
                            module.sourceDirs.addAll(sourceDir.srcDirs)
                        }
                    }
                }
            }
        }

    }
}
