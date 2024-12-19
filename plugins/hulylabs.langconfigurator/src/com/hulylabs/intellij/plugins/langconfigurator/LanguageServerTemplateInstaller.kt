// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.hulylabs.intellij.plugins.langconfigurator.nodejs.NodeRuntime
import com.hulylabs.intellij.plugins.langconfigurator.templates.HulyLanguageServerTemplate
import com.hulylabs.intellij.plugins.langconfigurator.utils.DecompressUtils
import com.hulylabs.intellij.plugins.langconfigurator.utils.DownloadUtils
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.*
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportRawProgress
import com.intellij.util.io.createDirectories
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

object LanguageServerTemplateInstaller {
  private final val LOG = Logger.getInstance(LanguageServerTemplateInstaller::class.java)

  @JvmStatic
  fun install(
    project: Project,
    template: HulyLanguageServerTemplate,
    onSuccess: Runnable,
    onFailure: (message: String) -> Unit,
  ) {
    (project as ComponentManagerEx).getCoroutineScope().launch {
      try {
        withBackgroundProgress(project, "Install binary") {
          installBinary(project, template)
        }
        withContext(Dispatchers.EDT) {
          addTemplate(project, template)
          onSuccess.run()
        }
      }
      catch (ex: IOException) {
        LOG.warn(ex.message, ex.cause)
        var sb = StringBuilder()
        sb.append(ex.message ?: "Unknown error")
        if (ex.cause?.message != null) {
          sb.append("\n")
          sb.append(ex.cause?.message.toString())
        }
        withContext(Dispatchers.EDT) {
          onFailure(sb.toString())
        }
      }
    }
  }

  @Throws(IOException::class)
  private suspend fun installBinary(project: Project, template: HulyLanguageServerTemplate) {
    LOG.info("Install binary for template ${template.id}")
    if (template.installCommand != null) {
      executeCommand(project, template)
    }
    else if (template.installNodeModules != null && !template.installNodeModules.isEmpty()) {
      val directory = Path.of(PathManager.getConfigPath(), "lsp4ij", template.id)
      if (!directory.exists()) {
        directory.createDirectories()
      }
      NodeRuntime.instance().npmInstallPackages(directory, *template.installNodeModules.toTypedArray())
    }
    else if (template.binaryUrl != null) {
      downloadBinary(project, template)
    }
  }

  @Throws(IOException::class)
  private suspend fun downloadBinary(project: Project, template: HulyLanguageServerTemplate) {
    LOG.info("Download binary")
    val directory = Path.of(PathManager.getConfigPath(), "lsp4ij", template.id)
    if (!directory.exists()) {
      directory.createDirectories()
    }
    val downloadName: String
    try {
      downloadName = URL(template.binaryUrl!!).file
    }
    catch (e: MalformedURLException) {
      throw IOException("Can't parse binary url", e)
    }
    val file = DownloadUtils.downloadFile(template.binaryUrl!!, downloadName, directory.toFile())
    DecompressUtils.decompress(file.toPath(), directory)
  }

  @Throws(IOException::class)
  suspend fun executeCommand(project: Project, template: HulyLanguageServerTemplate) {
    val command = template.installCommand
    val commandParts = command.split(" ").dropLastWhile { it.isEmpty() }.toTypedArray()
    val exePath = PathEnvironmentVariableUtil.findExecutableInWindowsPath(commandParts[0])
    val commandLine =
      GeneralCommandLine().withExePath(exePath).withParameters(Arrays.stream(commandParts).skip(1).toList())
        .withWorkDirectory(project.basePath).withCharset(StandardCharsets.UTF_8).withRedirectErrorStream(true)
    LOG.info("Install command: " + commandLine.commandLineString)
    val processHandler: ProcessHandler
    try {
      processHandler = OSProcessHandler(commandLine)
    }
    catch (e: ExecutionException) {
      throw IOException("Cannot execute command $commandLine", e)
    }

    val runner = CapturingProcessRunner(processHandler)
    reportRawProgress { reporter ->
      withContext(Dispatchers.IO) {
        reporter.text("Execute command")
        processHandler.addProcessListener(object : ProcessAdapter() {
          override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            LOG.info(event.text)
            reporter.details(event.text)
          }

          override fun processTerminated(event: ProcessEvent) {
            reporter.text("Command finished")
          }
        })
        val output = runner.runProcess(100_000, true)
        if (output.exitCode != 0) {
          LOG.warn("Install command failed with exit code: " + output.exitCode)
          throw IOException("Install command failed with exit code: " + output.exitCode)
        }
      }
    }
  }

  private fun addTemplate(project: Project, template: HulyLanguageServerTemplate) {
    LOG.info("Add template ${template.id}")
    val serverId = UUID.randomUUID().toString()

    val definition =
      UserDefinedLanguageServerDefinition(
        serverId,
        template.name,
        "",
        template.commandLine,
        emptyMap(),
        false,
        template.settingsJson,
        null,
        template.initializationOptionsJson,
        template.clientSettingsJson
      )
    LanguageServersRegistry.getInstance().addServerDefinition(project, definition, template.serverMappingSettings)
  }
}
