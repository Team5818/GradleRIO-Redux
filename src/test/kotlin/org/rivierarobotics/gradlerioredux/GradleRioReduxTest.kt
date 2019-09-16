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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GradleRioReduxTest {

    sealed class GradleVersion {
        object Current : GradleVersion() {
            override fun toString() = "Gradle Version: Current"
        }

        class String(val version: kotlin.String) : GradleVersion() {
            override fun toString() = "Gradle Version: $version"
        }
    }

    companion object {
        @get:JvmStatic
        val gradleVersions: Set<GradleVersion> = when (System.getenv("CI")) {
            "true", "1" -> setOf("5.5.1").map(GradleVersion::String).toSet()
            else -> setOf(GradleVersion.Current)
        }
    }

    lateinit var buildFile: Path

    private fun makeBuildFile(testProjectDir: Path) {
        buildFile = testProjectDir.resolve("build.gradle.kts")
        Path::class.java.kotlin
        Files.writeString(buildFile, """
                    plugins {
                        id("org.rivierarobotics.gradlerioredux")
                    }
                    gradleRioRedux {
                        robotClass = "org.rr.Robot"
                        teamNumber = 5818
                    }
                """.trimIndent())
    }

    @ParameterizedTest
    @MethodSource(value = ["getGradleVersions"])
    fun gradleCanConfigureProject(
        gradleVersion: GradleVersion,
        @TempDir testProjectDir: Path
    ) {
        makeBuildFile(testProjectDir)

        val result = GradleRunner.create()
            .also {
                if (gradleVersion is GradleVersion.String) {
                    it.withGradleVersion(gradleVersion.version)
                }
            }
            .withProjectDir(testProjectDir.toFile())
            .withArguments("tasks", "-Si", "--debug")
            .withPluginClasspath()
            .forwardOutput()
            .build()

        val tasksTask = result.task(":tasks")
        assertNotNull(tasksTask)
        assertEquals(tasksTask.outcome, TaskOutcome.SUCCESS)
    }

    @ParameterizedTest
    @MethodSource(value = ["getGradleVersions"])
    fun gradleCanBuildProject(
        gradleVersion: GradleVersion,
        @TempDir testProjectDir: Path
    ) {
        makeBuildFile(testProjectDir)
        val srcFile = testProjectDir.resolve("src/main/java/org/rr/Robot.java")
        Files.createDirectories(srcFile.parent)
        Files.writeString(srcFile, """
                        package org.rr;
                        import edu.wpi.first.wpilibj.TimedRobot;
                        public class Robot extends TimedRobot {}
                    """.trimIndent())

        val result = GradleRunner.create()
            .also {
                if (gradleVersion is GradleVersion.String) {
                    it.withGradleVersion(gradleVersion.version)
                }
            }
            .withProjectDir(testProjectDir.toFile())
            .withArguments("build", "-Si")
            .withPluginClasspath()
            .forwardOutput()
            .build()

        val tasksTask = result.task(":build")
        assertNotNull(tasksTask)
        assertEquals(tasksTask.outcome, TaskOutcome.SUCCESS)
    }
}