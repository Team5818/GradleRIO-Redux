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

import com.google.common.hash.Funnels
import com.google.common.hash.Hashing
import java.io.InputStream
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path

private fun hasher() = Hashing.murmur3_128().newHasher()

private inline fun hashInputStream(inputStream: () -> InputStream) = hasher().run {
    inputStream().use {
        it.copyTo(Funnels.asOutputStream(this))
    }
    hash().toString()
}

private val HTTP_CLIENT by lazy {
    HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
}

fun URL.openStreamFollowRedirects(): InputStream {
    return HTTP_CLIENT.send(
        HttpRequest.newBuilder(toURI()).build(),
        HttpResponse.BodyHandlers.ofInputStream()
    ).body()
}

private fun hashUrl(url: URL) = hashInputStream { url.openStreamFollowRedirects() }

private fun hashFile(file: Path) = hashInputStream { Files.newInputStream(file) }

data class DownloadInfo(val file: Path, val url: URL) {
    val upToDate by lazy {
        hashFile(file) == hashUrl(url)
    }
}
