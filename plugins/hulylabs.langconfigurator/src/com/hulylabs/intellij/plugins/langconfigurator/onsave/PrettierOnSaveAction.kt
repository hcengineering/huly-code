// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.hulylabs.intellij.plugins.langconfigurator.nodejs.NodeRuntime
import com.hulylabs.intellij.plugins.langconfigurator.utils.NodeUtils
import com.hulylabs.intellij.plugins.langconfigurator.utils.TTLCache
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.DocumentUpdatingActionOnSave
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

private const val WAIT_PRETTIER_TIMEOUT = 1000L

class PrettierOnSaveAction : DocumentUpdatingActionOnSave() {
  private val prettierRelativeBinPath = Path.of("node_modules", "prettier", "bin", "prettier.cjs")

  private var nodejs: NodeRuntime? = null
  private var configPaths: TTLCache<String, String> = TTLCache()
  private var prettierBinPaths: TTLCache<String, Path> = TTLCache()

  override fun isEnabledForProject(project: Project): Boolean {
    val settings = project.service<PrettierOnSaveSettings>().state
    return settings.isEnabled
  }

  override val presentableName: String
    get() = "Prettier"

  override suspend fun updateDocument(project: Project, document: Document) {
    val file = FileDocumentManager.getInstance().getFile(document)
    val settings = project.service<PrettierOnSaveSettings>().state
    if (file == null || !settings.isFileSupported(project, file)) {
      return
    }
    if (nodejs == null) {
      nodejs = NodeRuntime.Companion.instance()
    }

    var prettierBinPath = locatePrettierBinPath(file)
    if (prettierBinPath == null) {
      return
    }
    var configPath = getPrettierConfigPath(prettierBinPath, file)
    var cmd = GeneralCommandLine(nodejs!!.binaryPath().toString())
    cmd.addParameter(prettierBinPath.toString())
    cmd.addParameters("--no-color")
    if (configPath != null) {
      cmd.addParameters("--config", configPath)
    }
    cmd.addParameters("--stdin-filepath", file.path)
    cmd.withWorkingDirectory(Path.of(file.parent.path))
    cmd.environment["PATH"] = nodejs!!.binaryPath().parent.toString().replace('\\', '/')

    val handler = KillableProcessHandler(cmd)
    var formattedText = ""
    var errorText = ""
    var isSuccess = false
    handler.addProcessListener(object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        when (outputType) {
          ProcessOutputType.STDERR -> {
            errorText += event.text
          }
          ProcessOutputType.STDOUT -> {
            formattedText += event.text
          }
        }
      }

      override fun processTerminated(event: ProcessEvent) {
        if (event.exitCode != 0) {
          NotificationGroupManager.getInstance().getNotificationGroup("Prettier")
            .createNotification("Prettier", errorText, NotificationType.ERROR)
        }
        else {
          isSuccess = true
        }
      }
    })
    handler.startNotify()
    handler.processInput.write(document.text.toByteArray())
    handler.processInput.close()
    if (!handler.waitFor(WAIT_PRETTIER_TIMEOUT)) {
      handler.destroyProcess()
    }
    if (isSuccess) {
      writeCommandAction(project, "Format File") {
        document.setText(formattedText)
      }
    }
  }

  private suspend fun locatePrettierBinPath(file: VirtualFile): Path? {
    var prettierBinPath = prettierBinPaths.get(file.path)
    if (prettierBinPath != null) {
      return prettierBinPath
    }
    // try to find prettier bin path in node_modules of the current module
    val nodeModuleRootDir = NodeUtils.findNodeModuleRootDir(file)
    if (nodeModuleRootDir != null && nodeModuleRootDir.resolve(prettierRelativeBinPath).exists()) {
      prettierBinPath = nodeModuleRootDir.resolve(prettierRelativeBinPath)
      prettierBinPaths.put(file.path, prettierBinPath)
      return prettierBinPath
    }

    // try to install prettier in the config path
    val prettierHomeDir = Path.of(PathManager.getConfigPath(), "prettier")
    if (!prettierHomeDir.exists()) {
      prettierHomeDir.createDirectories()
      nodejs!!.npmInstallPackages(prettierHomeDir, "prettier", "--save-dev")
    }
    prettierBinPath = prettierHomeDir.resolve(prettierRelativeBinPath)
    prettierBinPaths.put(file.path, prettierBinPath)
    return prettierBinPath
  }

  private fun getPrettierConfigPath(prettierBinPath: Path, file: VirtualFile): String? {
    var configPath = configPaths.get(file.path)
    if (configPath != null) {
      return configPath
    }
    var cmd = GeneralCommandLine(nodejs!!.binaryPath().toString())
    cmd.addParameter(prettierBinPath.toString())
    cmd.addParameters("--no-color")
    cmd.addParameters("--find-config-path", file.path)
    cmd.withWorkingDirectory(Path.of(file.parent.path))
    cmd.environment["PATH"] = nodejs!!.binaryPath().parent.toString().replace('\\', '/')
    val processOutput = CapturingProcessHandler(cmd).runProcess(5000, true)
    configPath = processOutput.stdout.trim()
    if (configPath.isNotEmpty()) {
      configPaths.put(file.path, configPath)
      return configPath
    }
    return null
  }
}