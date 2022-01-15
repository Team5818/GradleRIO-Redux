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
import edu.wpi.first.deployutils.deploy.DeployExtension
import edu.wpi.first.deployutils.deploy.artifact.FileTreeArtifact
import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.gradlerio.deploy.roborio.FRCJavaArtifact
import edu.wpi.first.gradlerio.deploy.roborio.RoboRIO
import edu.wpi.first.toolchain.NativePlatforms
import org.cadixdev.gradle.licenser.LicenseExtension
import org.cadixdev.gradle.licenser.Licenser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.rivierarobotics.gradlerioredux.internal.frc
import org.rivierarobotics.gradlerioredux.internal.iterableAdd
import org.rivierarobotics.gradlerioredux.internal.wpi
import org.rivierarobotics.gradlerioredux.tasks.CheckVendorDeps
import org.rivierarobotics.gradlerioredux.tasks.PathWeaverCompile
import org.rivierarobotics.gradlerioredux.tasks.PathWeaverSourceSetExtension
import org.rivierarobotics.gradlerioredux.tasks.UpdateVendorDeps

internal val Project.rioExt
    get() = the<GradleRioReduxExtension>()

private const val TASK_GROUP = "GradleRIO-Redux"
const val PATH_WEAVER_CONFIGURATION = "grrPathWeaver"

class GradleRioRedux : Plugin<Project> {
    private lateinit var mainGeneration: TaskProvider<RobotMainGeneration>
    private lateinit var mainJavaCompile: TaskProvider<JavaCompile>

    override fun apply(project: Project) {
        project.run {
            extensions.create<GradleRioReduxExtension>("gradleRioRedux", project)
            applyPlugins()
            setupTasks()
            configureCheckstyle()
            setupPathWeaverSourceProcessing()
        }
    }

    private fun Project.applyPlugins() {
        apply(plugin = "java")

        configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(11))
        }

        if (rootProject.file("HEADER.txt").exists()) {
            apply<Licenser>()
            configure<LicenseExtension> {
                exclude {
                    it.file.startsWith(project.buildDir)
                }
                header(rootProject.file("HEADER.txt"))
                (this as ExtensionAware).extra.apply {
                    for (key in listOf("name", "organization", "url")) {
                        set(key, rootProject.property(key))
                    }
                }
            }
        }

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
                        it
                    )
                }
            }
        }
        configure<CheckstyleExtension> {
            config = resources.text.fromFile(checkstyleConfig)
        }
        rioExt.checkstyleVersionProperty.convention("9.2.1")
        rioExt.sevntuVersionProperty.convention("1.40.0")
        dependencies {
            "checkstyle"(rioExt.checkstyleVersionProperty.map { "com.puppycrawl.tools:checkstyle:$it" })
            "checkstyle"(rioExt.sevntuVersionProperty.map { "com.github.sevntu-checkstyle:sevntu-checks:$it" })
        }
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
            destinationDirectory.set(
                project.layout.buildDirectory.dir("${mgen.name}/classes")
            )
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

    fun applyGradleRioConfiguration(project: Project) {
        with(project) {
            apply<GradleRIOPlugin>()
            setupDeploy()
            setupDependencies()
            setupFatJar()

            wpi.sim.addGui().defaultEnabled.set(true)
            wpi.sim.addDriverstation().defaultEnabled.set(true)

            wpi.java.configureExecutableTasks(tasks.getByName<Jar>("jar"))
            wpi.java.configureTestTasks(tasks.getByName<Test>("test"))
        }
    }

    private fun Project.setupDeploy() {
        configure<DeployExtension> {
            targets.register<RoboRIO>("roboRio") {
                team = project.rioExt.teamNumber
                debug.set(frc.getDebugOrDefault(false))
                artifacts.register<FRCJavaArtifact>("frcJava") {
                    setJarTask(tasks.named<Jar>("jar"))
                }
                artifacts.register<FileTreeArtifact>("frcStaticFileDeploy") {
                    files.set(project.fileTree("src/main/deploy"))
                    directory.set("/home/lvuser/deploy")
                }
            }
        }
    }

    private fun Project.setupDependencies() {
        configurations.create(PATH_WEAVER_CONFIGURATION)
        dependencies {
            iterableAdd("implementation", wpi.java.deps.wpilib())
            iterableAdd("implementation", wpi.java.vendor.java())

            iterableAdd("roborioDebug", wpi.java.deps.wpilibJniDebug(NativePlatforms.roborio))
            iterableAdd("roborioDebug", wpi.java.vendor.jniDebug(NativePlatforms.roborio))

            iterableAdd("roborioRelease", wpi.java.deps.wpilibJniRelease(NativePlatforms.roborio))
            iterableAdd("roborioRelease", wpi.java.vendor.jniRelease(NativePlatforms.roborio))

            iterableAdd("nativeDebug", wpi.java.deps.wpilibJniDebug(NativePlatforms.desktop))
            iterableAdd("nativeDebug", wpi.java.vendor.jniDebug(NativePlatforms.desktop))
            iterableAdd("simulationDebug", wpi.sim.enableDebug())

            iterableAdd("nativeRelease", wpi.java.deps.wpilibJniRelease(NativePlatforms.desktop))
            iterableAdd("nativeRelease", wpi.java.vendor.jniRelease(NativePlatforms.desktop))
            iterableAdd("simulationRelease", wpi.sim.enableRelease())

            "testImplementation"("junit:junit:4.12")

            PATH_WEAVER_CONFIGURATION("edu.wpi.first.tools:PathWeaver:${wpi.versions.pathWeaverVersion}:${wpi.toolsClassifier}")
        }

        wpi.java.debugJni.set(false)
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

            exclude("META-INF/LICENSE")
            exclude("META-INF/NOTICE")
            exclude("module-info.class")

            manifest(GradleRIOPlugin.javaManifest(mainGeneration.get().mainClassFqn.get()))
        }
    }

    private fun Project.setupPathWeaverSourceProcessing() {
        the<JavaPluginExtension>().sourceSets.forEach { sourceSet ->
            with(extensions) {
                val extension: PathWeaverSourceSetExtension =
                    findByType() ?: create("pathWeaverSourceSets")
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
                    configure<IdeaModel> {
                        module.generatedSourceDirs.add(sourceDir.destinationDirectory.asFile.get())
                        module.sourceDirs.addAll(sourceDir.srcDirs)
                    }
                }
            }
        }

    }
}
