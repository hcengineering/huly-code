// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.utils

import com.intellij.util.io.Decompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.deleteExisting
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

object DecompressUtils {

  @JvmStatic
  @Throws(IOException::class)
  suspend fun decompress(filePath: Path, targetDir: Path, targetFileName: String? = null) {
    val fileName = filePath.name.lowercase()
    if (fileName.endsWith(".zip")) {
      withContext(Dispatchers.IO) {
        Decompressor.Zip(filePath).extract(targetDir)
      }
    }
    else if (fileName.endsWith(".tar")) {
      withContext(Dispatchers.IO) {
        Decompressor.Tar(filePath).extract(targetDir)
      }
    }
    else if (fileName.endsWith("tar.gz") || fileName.endsWith(".tgz")) {
      decompressTgz(filePath, targetDir)
    }
    else if (fileName.endsWith(".gz")) {
      decompressGz(filePath, targetDir, targetFileName)
    }
    else if (fileName.endsWith("tar.xz") || fileName.endsWith(".txz")) {
      decompressTxz(filePath, targetDir)
    }
    else {
      throw IOException("Unsupported archive type: $fileName")
    }
    filePath.deleteExisting()
  }

  @JvmStatic
  @Throws(IOException::class)
  suspend fun decompressTgz(filePath: Path, targetDir: Path) {
    withContext(Dispatchers.IO) {
      GZIPInputStream(BufferedInputStream(Files.newInputStream(filePath))).use { gzipInputStream ->
        val tarFilePath = filePath.parent.resolve(filePath.nameWithoutExtension)
        Files.copy(gzipInputStream, tarFilePath)
        Decompressor.Tar(tarFilePath).extract(targetDir)
        tarFilePath.deleteExisting()
      }
    }
  }

  @JvmStatic
  @Throws(IOException::class)
  suspend fun decompressGz(filePath: Path, targetDir: Path, targetFileName: String? = null) {
    withContext(Dispatchers.IO) {
      GZIPInputStream(BufferedInputStream(Files.newInputStream(filePath))).use { gzipInputStream ->
        Files.copy(gzipInputStream, targetDir.resolve(targetFileName ?: filePath.nameWithoutExtension))
      }
    }
  }

  @JvmStatic
  @Throws(IOException::class)
  suspend fun decompressTxz(filePath: Path, targetDir: Path) {
    withContext(Dispatchers.IO) {
      XZInputStream(BufferedInputStream(Files.newInputStream(filePath))).use { gzipInputStream ->
        val tarFilePath = filePath.parent.resolve(filePath.nameWithoutExtension)
        Files.copy(gzipInputStream, tarFilePath)
        Decompressor.Tar(tarFilePath).extract(targetDir)
        tarFilePath.deleteExisting()
      }
    }
  }
}