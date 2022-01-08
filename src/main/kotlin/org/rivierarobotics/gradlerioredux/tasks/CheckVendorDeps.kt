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

package org.rivierarobotics.gradlerioredux.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.rivierarobotics.gradlerioredux.internal.wpiVendorDownloadInfo

/**
 * Task to check the vendor dep JSON files for updates from their respective JSON URLs.
 */
open class CheckVendorDeps : DefaultTask() {
    @TaskAction
    fun checkVendorDeps() {
        for (info in project.wpiVendorDownloadInfo()) {
            if (info.upToDate) {
                logger.lifecycle("${info.file.fileName} is up-to-date.")
            } else {
                logger.lifecycle("Update for file ${info.file.fileName} at ${info.url}.")
            }
        }
        logger.lifecycle("Run 'updateVendorDeps' to update these files.")
    }
}
