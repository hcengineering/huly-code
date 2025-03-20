// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

private val supermavenDirectory = PathManager.getSystemDir().resolve("supermaven")
private val LOG = Logger.getInstance("#supermaven.runtime")

private const val WEEK_PERIOD_IN_MILLIS = 1000 * 60 * 60 * 24 * 7
private const val FORCE_CHECK_VERSION = 1

class SupermavenRuntime {

  @Serializable
  data class SupermavenDownloadInfo(val version: Long, val downloadUrl: String, val sha256Hash: String)

  @Throws(IOException::class)
  private suspend fun getDownloadInfo(project: Project, platform: String, arch: String): SupermavenDownloadInfo {
    return withBackgroundProgress(project, "Retrieve Supermaven agent download URL", false) {
      val url = "https://supermaven.com/api/download-path-v2?platform=${platform}&arch=${arch}&editor=hulycode"
      val response = try {
        LOG.info("requesting $url")
        HttpRequests.request(url).readString()
      }
      catch (e: IOException) {
        throw IOException("Unable to discover Supermaven Agent url ", e)
      }
      try {
        Json.decodeFromString(response)
      }
      catch (e: Exception) {
        throw IOException("Unable to parse url response", e)
      }
    }
  }

  @Throws(IOException::class)
  suspend fun getAgentPath(project: Project): Path {
    withContext(Dispatchers.IO) {
      Files.createDirectories(supermavenDirectory)
    }
    val platform = if (SystemInfo.isWindows) "windows" else (if (SystemInfo.isMac) "darwin" else (if (SystemInfo.isLinux) "linux" else ""))
    val arch = (if (CpuArch.isArm64()) "arm64" else (if (CpuArch.isIntel64()) "amd64" else ""))
    if (platform == "") {
      throw IOException("Unsupported platform")
    }
    if (arch == "") {
      throw IOException("Unsupported architecture")
    }
    var settings = ApplicationManager.getApplication().service<SupermavenSettings>()
    if (settings.state.agentVersionLastCheckTime + WEEK_PERIOD_IN_MILLIS < System.currentTimeMillis()
        || settings.state.forceCheckVersion != FORCE_CHECK_VERSION) {
      LOG.info("Checking for new Supermaven Agent version")
      val info = getDownloadInfo(project, platform, arch)
      settings.state.forceCheckVersion = FORCE_CHECK_VERSION
      settings.state.agentVersion = info.version
      settings.state.agentDownloadUrl = info.downloadUrl
      settings.state.agentVersionLastCheckTime = System.currentTimeMillis()
    }
    val downloadInfo = SupermavenDownloadInfo(settings.state.agentVersion, settings.state.agentDownloadUrl!!, "")
    val exeSuffix = if (SystemInfo.isWindows) ".exe" else ""
    val fileName = "sm-agent-${downloadInfo.version}${exeSuffix}"
    val binaryPath = supermavenDirectory.resolve(fileName)
    if (Files.exists(binaryPath)) {
      LOG.info("Supermaven Agent already exists at $binaryPath")
      return binaryPath
    }

    val pairs = withBackgroundProgress(project, "Downloading Supermaven agent", false) {
      try {
        val service = DownloadableFileService.getInstance()
        val description = service.createFileDescription(downloadInfo.downloadUrl, fileName)
        service.createDownloader(listOf(description), fileName).downloadWithBackgroundProgress(supermavenDirectory.toString(), project)
      }
      catch (e: IOException) {
        throw IOException("Unable to download Supermaven Agent", e)
      }
    }.await()
    val resFilePath = pairs?.firstOrNull()?.first
    LOG.info("Supermaven Agent downloaded to $resFilePath")
    if (!SystemInfo.isWindows) {
      binaryPath.toFile().setExecutable(true)
    }

    withContext(Dispatchers.IO) {
      Files.walk(supermavenDirectory, 1).forEach {
        if (Files.isRegularFile(it) && it.toString() != binaryPath.toString()) {
          @Suppress("BlockingMethodInNonBlockingContext")
          Files.deleteIfExists(it)
        }
      }
    }
    return binaryPath
  }
}