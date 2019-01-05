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

import okhttp3.Cache
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Okio
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit

class PullJsonDependency(cacheDir: Path,
                         private val jsonOutDir: Path,
                         private val useNetwork: Boolean) {

    private val logger: Logger = Logging.getLogger("GradleRIO-Redux-JsonDL")
    private val jsonDownloadCache: Path = cacheDir.resolve("jsondl-cache").also { Files.createDirectories(it) }
    private val okHttpCache: Path = cacheDir.resolve("okhttp-cache").also { Files.createDirectories(it) }
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
            .cache(Cache(okHttpCache.toFile(), 1024 * 1024))
            .connectionPool(ConnectionPool(5, 60, TimeUnit.SECONDS))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

    private fun String.downloadFailed(reason: String): Nothing {
        throw IllegalStateException("Dependency download from `${this}` failed: $reason")
    }

    /**
     * Download the JSON dependency info at {@code url} and cache it in {@code jsonOutDir}.
     */
    fun downloadDependencyIfNeeded(url: String) {
        val httpUrl = HttpUrl.get(url)
        val downloadTarget = httpUrl.pathSegments().last().let {
            when {
                it.endsWith(".json") -> it
                else -> "$it.json"
            }
        }
        val downloadCacheFile = jsonDownloadCache.resolve(downloadTarget)
        // HEAD first, and check modification times
        if (useNetwork) {
            if (!alreadyDownloaded(httpUrl, downloadCacheFile)) {
                logger.lifecycle("Downloading JSON dependency $downloadTarget from $httpUrl")
                download(httpUrl, downloadCacheFile)
            }
        } else {
            logger.lifecycle("Note: not checking $downloadTarget remote due to offline mode.")
        }

        // now copy it to the local project cache
        val downloadOutFile = jsonOutDir.resolve(downloadTarget)
        if (!alreadyCopied(downloadCacheFile, downloadOutFile)) {
            logger.lifecycle("Copying JSON dependency $downloadTarget to project")
            copy(downloadTarget, downloadCacheFile, downloadOutFile)
        }
    }

    private fun alreadyDownloaded(httpUrl: HttpUrl, downloadCacheFile: Path): Boolean {
        val response = httpClient.newCall(Request.Builder()
                .head().url(httpUrl)
                .build()).simpleErrorHandlingExecute()
        val remoteLastModified = response.headers().getDate("Last-Modified")?.time
        val localLastModified = downloadCacheFile.lastModifiedTimeIfExists()
        if (remoteLastModified == null || localLastModified == null) {
            return false
        }
        return localLastModified >= remoteLastModified
    }

    private fun Path.lastModifiedTimeIfExists(): Long? {
        return takeIf(Files::exists)?.let {
            Files.getLastModifiedTime(it).toMillis()
        }
    }

    private fun download(httpUrl: HttpUrl, downloadCacheFile: Path) {
        val response = httpClient.newCall(Request.Builder()
                .get().url(httpUrl)
                .build()).simpleErrorHandlingExecute()
        val body = response.body() ?: httpUrl.toString().downloadFailed("No response body")
        Okio.buffer(Okio.sink(downloadCacheFile)).use { snk ->
            body.source().use(snk::writeAll)
        }
        response.headers().getDate("Last-Modified")?.let {
            Files.setLastModifiedTime(downloadCacheFile, FileTime.fromMillis(it.time))
        }
    }

    private fun alreadyCopied(source: Path, target: Path): Boolean {
        val srcLastMod = source.lastModifiedTimeIfExists()
        val dstLastMod = target.lastModifiedTimeIfExists()
        if (srcLastMod == null || dstLastMod == null) {
            return false
        }
        return dstLastMod >= srcLastMod
    }

    private fun copy(downloadTarget: String, source: Path, target: Path) {
        if (!Files.exists(source) && !Files.exists(target)) {
            throw IllegalStateException("Vendor dependency unavailable: $downloadTarget. Try with online mode.")
        }
        Files.copy(source, target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES)
    }

    private fun Call.simpleErrorHandlingExecute(): Response {
        val response = execute()
        if (!response.isSuccessful) {
            val message = response.body()?.string() ?: "<No message>"
            response.request().url().toString().downloadFailed("${response.code()} $message")
        }
        return response
    }
}
