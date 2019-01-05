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

import org.gradle.api.file.RegularFile
import org.gradle.api.internal.AbstractTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class RobotMainGeneration : AbstractTask() {
    @get:Input
    val mainClassFqn: Property<String> = project.objects.property<String>().also {
        it.set("org.rivierarobotics.robot.Main")
    }
    @get:Input
    val robotClass: Property<String> = project.objects.property()
    @get:OutputFile
    val outputFile: Property<RegularFile> = project.objects.fileProperty().apply {
        set(project.layout.buildDirectory.dir("$name/generated/java").flatMap { dir ->
            dir.file(mainClassFqn.map { it.replace('.', '/') + ".java" })
        })
    }

    @TaskAction
    fun generateRobotMain() {
        val parts = mainClassFqn.get().split('.')
        val pkg = parts.dropLast(1).joinToString(".")
        val name = parts.last()
        outputFile.get().asFile.writeText("""
            package $pkg;

            import edu.wpi.first.wpilibj.RobotBase;

            public final class $name {
                private $name() {
                }

                public static void main(String[] args) {
                    RobotBase.startRobot(${robotClass.get()}::new);
                }
            }
        """.trimIndent())
    }
}