package org.rivierarobotics.gradlerioredux

import java.net.URL
import java.nio.file.Path

data class DownloadInfo(val file: Path, val url: URL)
