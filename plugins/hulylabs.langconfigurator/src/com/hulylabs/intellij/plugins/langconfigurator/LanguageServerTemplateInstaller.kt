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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportRawProgress
import com.intellij.util.io.createDirectories
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition
import com.redhat.devtools.lsp4ij.settings.ErrorReportingKind
import com.redhat.devtools.lsp4ij.settings.UserDefinedLanguageServerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

object LanguageServerTemplateInstaller {
  private val LOG = Logger.getInstance(LanguageServerTemplateInstaller::class.java)
  val TEMPLATE_PREFIX = "huly-code-"

  @JvmStatic
  fun install(
    project: Project,
    template: HulyLanguageServerTemplate,
    onSuccess: Runnable,
    onFailure: (message: String) -> Unit,
  ) {
    (project as ComponentManagerEx).getCoroutineScope().launch {
      try {
        var env = emptyMap<String, String>()
        withBackgroundProgress(project, "Install binary") {
          env = installBinary(project, template)
        }
        withContext(Dispatchers.EDT) {
          addTemplate(project, template, env)
          onSuccess.run()
        }
      }
      catch (ex: IOException) {
        LOG.warn(ex.message, ex.cause)
        val sb = StringBuilder()
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

  suspend fun cleanupOldVersions(template: HulyLanguageServerTemplate) {
    withContext(Dispatchers.IO) {
      if (template.version > 0) {
        for (version in template.version - 1 downTo 0) {
          val directory = getTemplateDirectory(template.id, version)
          if (directory.exists()) {
            LOG.info("Cleanup old version $version of template ${template.id}")
            try {
              NioFiles.deleteRecursively(directory)
            } catch (ex: IOException) {
              LOG.warn("Failed to cleanup old version $version of template ${template.id}", ex)
            }
          }
        }
      }
    }
  }

  @JvmStatic
  fun update(project: Project, serverDefinition: UserDefinedLanguageServerDefinition, template: HulyLanguageServerTemplate, onSuccess: Runnable, onFailure: (message: String) -> Unit) {
    (project as ComponentManagerEx).getCoroutineScope().launch {
      try {
        var env = emptyMap<String, String>()
        withBackgroundProgress(project, "Install binary") {
          env = installBinary(project, template)
        }
        withContext(Dispatchers.EDT) {
          updateTemplate(project, serverDefinition, template, env)
          onSuccess.run()
        }
        cleanupOldVersions(template)
      }
      catch (ex: IOException) {
        LOG.warn(ex.message, ex.cause)
        val sb = StringBuilder()
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

  private fun getTemplateDirectory(templateId: String, templateVersion: Long): Path {
    if (templateVersion > 0) {
      return Path.of(PathManager.getConfigPath(), "lsp4ij", templateId + "-v" + templateVersion)
    } else {
      return Path.of(PathManager.getConfigPath(), "lsp4ij", templateId)
    }
  }

  @Throws(IOException::class)
  private suspend fun installBinary(project: Project, template: HulyLanguageServerTemplate): Map<String, String> {
    LOG.info("Install binary for template ${template.id}")
    val env = mutableMapOf<String, String>()
    if (template.installCommand != null) {
      executeCommand(project, template)
    }
    else if (template.installNodeModules != null && template.installNodeModules.isNotEmpty()) {
      val directory = getTemplateDirectory(template.id, template.version)
      if (directory.exists()) {
        NioFiles.deleteRecursively(directory)
      }
      directory.createDirectories()
      val nodeRuntime = NodeRuntime.instance()
      nodeRuntime.npmInstallPackages(directory, *template.installNodeModules.toTypedArray())
      env["PATH"] = nodeRuntime.binaryPath().parent.toCanonicalPath()
      env["LSP_ROOT"] = directory.toCanonicalPath()
    }
    else if (template.binaryUrl != null) {
      downloadBinary(project, template)
    }
    else if (template.installGoPackages != null && template.installGoPackages.isNotEmpty()) {
      installGoPackages(project, template)
    } else if (template.installPythonPackages != null && template.installPythonPackages.isNotEmpty()) {
      env.putAll(installPythonPackages(project, template))
    }
    return env
  }

  @Throws(IOException::class)
  private suspend fun installGoPackages(
    project: Project,
    template: HulyLanguageServerTemplate,
  ) {
    val directory = getTemplateDirectory(template.id, template.version)
    if (directory.exists()) {
      NioFiles.deleteRecursively(directory)
    }
    directory.createDirectories()
    val exePath = PathEnvironmentVariableUtil.findExecutableInWindowsPath("go")
    val args = buildList<String>(template.installGoPackages.size + 1) {
      add("install")
      addAll(template.installGoPackages)
    }
    val installEnv = mutableMapOf<String, String>()
    installEnv["GOBIN"] = directory.toString()
    val commandLine =
      GeneralCommandLine().withExePath(exePath).withParameters(args).withEnvironment(installEnv)
        .withWorkDirectory(project.basePath).withCharset(StandardCharsets.UTF_8).withRedirectErrorStream(true)
    LOG.info("Run go install: " + commandLine.commandLineString)
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
          LOG.warn("go install command failed with exit code: " + output.exitCode)
          throw IOException("go install command failed with exit code: " + output.exitCode)
        }
      }
    }
  }

  @Throws(IOException::class)
  private suspend fun downloadBinary(project: Project, template: HulyLanguageServerTemplate) {
    LOG.info("Download binary")
    val directory = getTemplateDirectory(template.id, template.version)
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
    DecompressUtils.decompress(file.toPath(), directory, template.binaryExecutable)
    if (template.binaryExecutable != null) {
      FileUtil.setExecutable(directory.resolve(template.binaryExecutable).toFile())
    }
  }

  @Throws(IOException::class)
  private suspend fun installPythonPackages(project: Project, template: HulyLanguageServerTemplate): Map<String, String> {
    val directory = getTemplateDirectory(template.id, template.version)
    directory.createDirectories()
    val exePath = PathEnvironmentVariableUtil.findExecutableInWindowsPath("pip3")
    val args = buildList<String>(template.installPythonPackages.size + 4) {
      add("install")
      add("--upgrade")
      add("--target")
      add(directory.toCanonicalPath())
      addAll(template.installPythonPackages)
    }
    val commandLine =
      GeneralCommandLine().withExePath(exePath).withParameters(args).withCharset(StandardCharsets.UTF_8)
        .withRedirectErrorStream(true)
    LOG.info("Run pip install: " + commandLine.commandLineString)
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
          LOG.warn("pip install command failed with exit code: " + output.exitCode)
          throw IOException("pip install command failed with exit code: " + output.exitCode)
        }
      }
    }
    return mapOf("PYTHONPATH" to directory.toCanonicalPath())
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

  private fun addTemplate(project: Project, template: HulyLanguageServerTemplate, env: Map<String, String>) {
    LOG.info("Add template ${template.id}")
    val serverId = UUID.randomUUID().toString()
    val definition =
      UserDefinedLanguageServerDefinition(
        serverId,
        TEMPLATE_PREFIX + template.id + "-v" + template.version,
        template.name,
        "",
        template.commandLine.replace("\$TEMPLATE_DIR$", getTemplateDirectory(template.id, template.version).toCanonicalPath()),
        env,
        false,
        normalizeString(template.settingsJson, env),
        null,
        normalizeString(template.initializationOptionsJson, env),
        normalizeString(template.clientSettingsJson, env)
      )
    LanguageServersRegistry.getInstance().addServerDefinition(project, definition, template.serverMappingSettings)
    val settings = UserDefinedLanguageServerSettings.LanguageServerDefinitionSettings().setErrorReportingKind(ErrorReportingKind.none)
    UserDefinedLanguageServerSettings.getInstance(project).updateSettings(serverId, settings)
  }

  private fun mergeJson(oldJsonStr: String, newJsonStr: String): String {
    val oldJson: JsonObject
    val newJson: JsonObject
    try {
      oldJson = Json.parseToJsonElement(oldJsonStr).jsonObject
      newJson = Json.parseToJsonElement(newJsonStr).jsonObject
    } catch (e: Exception) {
      LOG.warn("Failed to parse json", e)
      return oldJsonStr
    }
    val resultJson = buildJsonObject {
      for ((key, value) in oldJson.entries) {
        put(key, value)
      }
      for ((key, value) in newJson.entries) {
        if (!oldJson.containsKey(key)) {
          put(key, value)
        }
      }
    }
    return resultJson.toString()
  }

  private fun updateTemplate(project: Project, languageServerDefinition: UserDefinedLanguageServerDefinition, template: HulyLanguageServerTemplate, env: Map<String, String>) {
    LOG.info("Update template ${template.id}")
    val configurationContent = normalizeString(template.settingsJson, env)?.let { mergeJson(languageServerDefinition.configurationContent, it) }
    val initializationOptionsContent = normalizeString(template.initializationOptionsJson, env)?.let { mergeJson(languageServerDefinition.initializationOptionsContent, it) }
    val update = LanguageServersRegistry.UpdateServerDefinitionRequest(
      project,
      languageServerDefinition,
      template.name,
      template.commandLine.replace("\$TEMPLATE_DIR$", getTemplateDirectory(template.id, template.version).toCanonicalPath()),
      env,
      false,
      template.serverMappingSettings,
      configurationContent,
      languageServerDefinition.configurationSchemaContent,
      initializationOptionsContent,
      normalizeString(template.clientSettingsJson, env),
      TEMPLATE_PREFIX + template.id + "-v" + template.version,
    )
    LanguageServersRegistry.getInstance().updateServerDefinition(update, true)
  }

  /**
   * Replaces all occurrences of ${key} with the value of the corresponding key in the environment.
   */
  private fun normalizeString(str: String?, env: Map<String, String>): String? {
    if (str == null) return null
    var res = str
    for ((key, value) in env) {
      res = res!!.replace("\${$key}", value)
    }
    return res
  }
}
