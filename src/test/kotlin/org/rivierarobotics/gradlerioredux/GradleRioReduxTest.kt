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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GradleRioReduxTest {
    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder()
    lateinit var buildFile: File

    private fun makeBuildFile() {
        buildFile = testProjectDir.newFile("build.gradle.kts")
        buildFile.writeText("""
                plugins {
                    id("org.rivierarobotics.gradlerioredux")
                }
                gradleRioRedux {
                    robotClass = "org.rr.Robot"
                    teamNumber = 5818
                    addCtre = true
                }
            """.trimIndent())
    }

    @Test
    fun gradleCanConfigureProject() {
        makeBuildFile()

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("tasks", "-Si")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        val tasksTask = result.task(":tasks")
        assertNotNull(tasksTask)
        assertEquals(tasksTask.outcome, TaskOutcome.SUCCESS)
    }

    @Test
    fun gradleCanBuildProject() {
        makeBuildFile()
        testProjectDir.newFolder("src", "main", "java", "org", "rr").resolve("Robot.java")
                .writeText("""
                    package org.rr;
                    import edu.wpi.first.wpilibj.TimedRobot;
                    public class Robot extends TimedRobot {}
                """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("build", "-Si")
                .withPluginClasspath()
                .forwardOutput()
                .build()

        val tasksTask = result.task(":build")
        assertNotNull(tasksTask)
        assertEquals(tasksTask.outcome, TaskOutcome.SUCCESS)
    }
}