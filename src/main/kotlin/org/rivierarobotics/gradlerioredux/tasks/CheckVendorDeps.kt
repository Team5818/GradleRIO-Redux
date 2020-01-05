package org.rivierarobotics.gradlerioredux.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.rivierarobotics.gradlerioredux.downloadInfo
import org.rivierarobotics.gradlerioredux.wpi

/**
 * Task to check the vendor dep JSON files for updates from their respective JSON URLs.
 */
open class CheckVendorDeps : DefaultTask() {
    @TaskAction
    fun updateVendorDeps() {
        for (info in project.wpi.deps.vendor.downloadInfo) {
            logger.lifecycle("Update for file ${info.file.fileName} at ${info.url}.")
        }
        logger.lifecycle("Run 'updateVendorDeps' to update these files.")
    }
}
