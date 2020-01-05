package org.rivierarobotics.gradlerioredux.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.rivierarobotics.gradlerioredux.downloadInfo
import org.rivierarobotics.gradlerioredux.wpi
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Task to update the vendor dep JSON files from their respective JSON URLs.
 */
open class UpdateVendorDeps : DefaultTask() {
    @TaskAction
    fun updateVendorDeps() {
        for (info in project.wpi.deps.vendor.downloadInfo) {
            val temp = Files.createTempFile("gradlerio-redux-download", ".json")
            info.url.openStream().use { source ->
                Files.newOutputStream(temp).use { sink ->
                    source.copyTo(sink)
                }
            }
            Files.move(temp, info.file, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
