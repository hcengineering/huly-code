// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.hulylabs.intellij.plugins.langconfigurator.nodejs.NodeRuntime
import com.hulylabs.intellij.plugins.langconfigurator.utils.NodeUtils
import com.hulylabs.intellij.plugins.langconfigurator.utils.TTLCache
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.DocumentUpdatingActionOnSave
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.removeUserData
import com.intellij.psi.PsiManager
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.io.path.exists

private const val WAIT_ESLINT_TIMEOUT = 10000L

class ESLintOnSaveAction : DocumentUpdatingActionOnSave() {
  private var nodejs: NodeRuntime? = null
  private val eslintBinPath = Path.of("node_modules", "eslint", "bin", "eslint.js")
  private var nodeModuleRootDirs: TTLCache<String, Path> = TTLCache()

  override fun isEnabledForProject(project: Project): Boolean {
    val settings = project.service<ESLintOnSaveSettings>().state
    return settings.isEnabled
  }

  override val presentableName: String
    get() = "ESLint"

  override suspend fun updateDocument(project: Project, document: Document) {
    val file = FileDocumentManager.getInstance().getFile(document)
    val settings = project.service<ESLintOnSaveSettings>().state
    if (file == null || !settings.isFileSupported(project, file)) {
      return
    }

    var nodeModuleRootDir = nodeModuleRootDirs.get(file.path)
    if (nodeModuleRootDir == null) {
      val rootDir = NodeUtils.findNodeModuleRootDir(file)
      if (rootDir != null && rootDir.resolve(eslintBinPath).exists()) {
        nodeModuleRootDir = rootDir
        nodeModuleRootDirs.put(file.path, nodeModuleRootDir)
      }
      else {
        return
      }
    }
    if (nodejs == null) {
      nodejs = NodeRuntime.Companion.instance()
    }

    var cmd = GeneralCommandLine(nodejs!!.binaryPath().toString())
    cmd.addParameter(nodeModuleRootDir.resolve(eslintBinPath).toString())
    cmd.addParameter("--no-color")
    cmd.addParameter("--fix-dry-run")
    cmd.addParameters("--format", "json")
    cmd.addParameter("--stdin")
    cmd.addParameters("--stdin-filename", file.path)
    cmd.withWorkingDirectory(nodeModuleRootDir)
    cmd.environment["PATH"] = nodejs!!.binaryPath().parent.toString().replace('\\', '/')

    val handler = KillableProcessHandler(cmd)
    var responseText = ""
    var errorText = ""
    var isSuccess = false
    handler.addProcessListener(object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        when (outputType) {
          ProcessOutputType.STDERR -> {
            errorText += event.text
          }
          ProcessOutputType.STDOUT -> {
            responseText += event.text
          }
        }
      }

      override fun processTerminated(event: ProcessEvent) {
        if (event.exitCode != 0 && errorText != "") {
          NotificationsManager.getNotificationsManager().showNotification(
            NotificationGroupManager.getInstance().getNotificationGroup("ESLint")
              .createNotification("ESLint", errorText, NotificationType.ERROR),
            project
          )
        }
        else {
          isSuccess = true
        }
      }
    })
    handler.startNotify()
    handler.processInput.write(document.text.toByteArray())
    handler.processInput.close()
    if (!handler.waitFor(WAIT_ESLINT_TIMEOUT)) {
      handler.destroyProcess()
    }
    if (isSuccess) {
      try {
        val responseArray: JsonArray = Json.decodeFromString(responseText)
        val responseObj: JsonObject = responseArray[0].jsonObject
        if (responseObj.containsKey("output")) {
          val output = responseObj["output"]
          val formattedText = output?.jsonPrimitive?.contentOrNull
          if (formattedText != null) {
            writeCommandAction(project, "Format File") {
              document.setText(formattedText)
            }
          }
        }
        if (responseObj.containsKey("messages")) {
          val messages = responseObj["messages"]!!.jsonArray
          document.putUserData(ESLINT_MESSAGES_KEY, messages)
        }
        else {
          document.removeUserData(ESLINT_MESSAGES_KEY)
        }
        val psiFile = readAction {
          PsiManager.getInstance(project).findFile(file)
        }
        if (psiFile != null) {
          DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
        }
      }
      catch (e: Exception) {
        NotificationsManager.getNotificationsManager().showNotification(
          NotificationGroupManager.getInstance().getNotificationGroup("ESLint")
            .createNotification("ESLint", e.message
                                          ?: "Error while formatting", NotificationType.ERROR), project
        )
      }
    }
  }
}