// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.utils

import com.intellij.platform.util.progress.reportRawProgress
import com.intellij.util.download.DownloadableFileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object DownloadUtils {

  @JvmStatic
  @Throws(IOException::class)
  suspend fun downloadFile(url: String, fileName: String, directory: File): File {
    val service = DownloadableFileService.getInstance()
    val description = service.createFileDescription(url, fileName)
    val downloader = service.createDownloader(listOf(description), fileName)
    return reportRawProgress { reporter ->
      try {
        reporter.text("Downloading $fileName")
        val file = withContext(Dispatchers.IO) {
          val pairs = downloader.download(directory)
          pairs.firstOrNull()?.first
        }
        file ?: throw IOException("No file downloaded")
      }
      catch (e: IOException) {
        throw IOException("Can't download file", e)
      }
    }
  }
}