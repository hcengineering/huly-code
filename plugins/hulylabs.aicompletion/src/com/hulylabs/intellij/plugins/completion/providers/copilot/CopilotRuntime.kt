// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot

import com.hulylabs.intellij.plugins.langconfigurator.utils.DecompressUtils
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.download.DownloadableFileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

private val LOG = Logger.getInstance("#copilot.runtime")
private const val COPILOT_LSP_VERSION = "0.7.0"
private const val COPILOT_LSP_BINARY_URL = "https://github.com/zed-industries/copilot/releases/download/v$COPILOT_LSP_VERSION/copilot.tar.gz"
private val copilotDirectory = Path.of(PathManager.getConfigPath(), "copilot")

class CopilotRuntime {

  @Throws(IOException::class)
  suspend fun getAgentPath(project: Project): Path {
    val versionFile = copilotDirectory.resolve("version.txt")
    val agentPath = copilotDirectory.resolve("language-server.js")
    if (Files.exists(versionFile) && Files.exists(agentPath)) {
      val version = withContext(Dispatchers.IO) {
        Files.readString(versionFile)
      }
      if (version == COPILOT_LSP_VERSION) {
        LOG.info("Copilot Agent version $version already exists at $copilotDirectory")
        return agentPath
      }
    }
    withContext(Dispatchers.IO) {
      Files.createDirectories(copilotDirectory)
    }

    val pairs = withBackgroundProgress(project, "Downloading Copilot LSP server", false) {
      try {
        val service = DownloadableFileService.getInstance()
        val description = service.createFileDescription(COPILOT_LSP_BINARY_URL, "copilot.tar.gz")
        service.createDownloader(listOf(description), "copilot.tar.gz").downloadWithBackgroundProgress(copilotDirectory.toString(), project)
      }
      catch (e: IOException) {
        throw IOException("Unable to download Copilot Agent", e)
      }
    }.await()
    val resFilePath = pairs?.firstOrNull()?.first
    LOG.info("Copilot Agent downloaded to $resFilePath")
    DecompressUtils.decompress(copilotDirectory.resolve("copilot.tar.gz"), copilotDirectory)
    withContext(Dispatchers.IO) {
      Files.writeString(versionFile, COPILOT_LSP_VERSION)
    }
    return agentPath
  }
}