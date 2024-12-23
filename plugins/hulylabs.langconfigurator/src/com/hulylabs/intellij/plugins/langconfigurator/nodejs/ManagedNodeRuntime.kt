// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.nodejs

import com.hulylabs.intellij.plugins.langconfigurator.utils.DecompressUtils
import com.hulylabs.intellij.plugins.langconfigurator.utils.DownloadUtils
import com.hulylabs.intellij.plugins.langconfigurator.utils.ExecuteUtils
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import com.intellij.util.io.createDirectories
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

private const val NODE_VERSION = "v22.12.0"

private val LOG = Logger.getInstance(ManagedNodeRuntime::class.java)

class ManagedNodeRuntime(private val installationPath: Path) : NodeRuntime() {
  private val nodeEnvironmentPath: String
    get() {
      val nodeBinaryPath = binaryPath().parent.toString()
      val pathEnv = EnvironmentUtil.getValue("PATH")
      return if (pathEnv == null) {
        nodeBinaryPath
      }
      else {
        pathEnv + (if (SystemInfo.isWindows) ';' else ':') + nodeBinaryPath
      }
    }

  override fun binaryPath(): Path {
    return installationPath.resolve(NODE_PATH)
  }

  @Throws(IOException::class)
  override suspend fun runNpmSubcommand(directory: Path?, subcommand: String, vararg args: String): String? {
    val nodeBinary = binaryPath()
    val npmFile = installationPath.resolve(NPM_PATH)
    val envPath = nodeEnvironmentPath

    if (!Files.exists(nodeBinary)) {
      throw IOException("Node binary not found at: $nodeBinary")
    }
    if (!Files.exists(npmFile)) {
      throw IOException("npm file not found at: $npmFile")
    }

    var commandLine =
      GeneralCommandLine()
        .withExePath(nodeBinary.toString())
        .withParameters(npmFile.toString())
        .withParameters(subcommand)
        .withParameters("--cache", installationPath.resolve("cache").toString())
        .withParameters("--userconfig", installationPath.resolve("blank_user_npmrc").toString())
        .withParameters("--globalconfig", installationPath.resolve("blank_global_npmrc").toString())
        .withEnvironment("PATH", envPath)
        .withParameters(*args)
    commandLine = configureNpmCommand(commandLine)
    return ExecuteUtils.execCommand("Execute npm ${subcommand}", commandLine, directory ?: installationPath, true)
  }

  private fun configureNpmCommand(origCommandLine: GeneralCommandLine): GeneralCommandLine {
    var commandLine = origCommandLine

    if (SystemInfo.isWindows) {
      val systemRoot = EnvironmentUtil.getValue("SYSTEMROOT")
      if (systemRoot != null) {
        commandLine = commandLine.withEnvironment("SYSTEMROOT", systemRoot)
      }
      val comSpec = EnvironmentUtil.getValue("ComSpec")
      if (comSpec != null) {
        commandLine = commandLine.withEnvironment("ComSpec", comSpec)
      }
    }
    return commandLine
  }

  companion object {
    private val NODE_PATH = if (SystemInfo.isWindows) "node.exe" else "bin/node"
    private val NPM_PATH = if (SystemInfo.isWindows) "node_modules/npm/bin/npm-cli.js" else "bin/npm"

    @JvmStatic
    @Throws(IOException::class)
    suspend fun installIfNeeded(): NodeRuntime {
      LOG.info("Node runtime installIfNeeded")
      val os = if (SystemInfo.isWindows) "win" else (if (SystemInfo.isMac) "darwin" else (if (SystemInfo.isLinux) "linux" else null))
      val arch = if (CpuArch.isArm64()) "arm64" else (if (CpuArch.isIntel64()) "x64" else null)

      if (os == null || arch == null) {
        throw IOException("Unsupported platform")
      }
      val folderName = String.format("node-%s-%s-%s", NODE_VERSION, os, arch)
      val nodeContainingDir = Path.of(PathManager.getConfigPath(), "node")
      val nodeDir = nodeContainingDir.resolve(folderName)
      val nodeBinary: Path = nodeDir.resolve(NODE_PATH)
      val npmFile: Path = nodeDir.resolve(NPM_PATH)

      try {
        ExecuteUtils.execCommand(
          "Check installed node",
          nodeBinary.toString(),
          nodeDir,
          false,
          npmFile.toString(),
          "--version",
          "--cache", nodeDir.resolve("cache").toString(),
          "--userconfig", nodeDir.resolve("blank_user_npmrc").toString(),
          "--globalconfig", nodeDir.resolve("blank_global_npmrc").toString()
        )
      }
      catch (_: IOException) {
        nodeContainingDir.toFile().deleteRecursively()
        nodeContainingDir.createDirectories()
        val archiveType = when {
          SystemInfo.isMac -> "tar.gz"
          SystemInfo.isLinux -> "tar.gz"
          SystemInfo.isWindows -> "zip"
          else -> throw IOException("Running on unsupported os: ${OS.CURRENT}")
        }
        val fileName = "node-$NODE_VERSION-${os}-${arch}.$archiveType"
        val url = "https://nodejs.org/dist/$NODE_VERSION/$fileName"
        DownloadUtils.downloadFile(url, fileName, nodeContainingDir.toFile())
        DecompressUtils.decompress(nodeContainingDir.resolve(fileName), nodeContainingDir)
      }
      return ManagedNodeRuntime(nodeDir)
    }
  }
}