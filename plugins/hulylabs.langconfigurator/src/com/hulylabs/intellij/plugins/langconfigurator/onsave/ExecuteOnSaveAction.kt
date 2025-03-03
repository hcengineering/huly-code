// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironmentBuilder.Companion.create
import com.intellij.execution.util.ExecutionErrorDialog
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.DocumentUpdatingActionOnSave
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ExecuteOnSaveAction() : DocumentUpdatingActionOnSave() {

  override val presentableName: String
    get() = "Execute External Tool"

  override fun isEnabledForProject(project: Project): Boolean {
    val settings = project.service<ExecuteOnSaveSettings>().state
    return settings.isEnabled && !settings.toolActionId.isNullOrEmpty()
  }

  override suspend fun updateDocument(project: Project, document: Document) {
    val file = FileDocumentManager.getInstance().getFile(document)
    val settings = project.service<ExecuteOnSaveSettings>().state
    if (file == null || !settings.isFileSupported(project, file)) {
      return
    }

    val toolActionId = settings.toolActionId!!
    val tool = ToolManager.getInstance().tools.firstOrNull { it.actionId == toolActionId }
    if (tool != null) {
      var context = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .add(CommonDataKeys.VIRTUAL_FILE, file)
        .build()
      executeTool(project, tool, context)
    }
  }

  private suspend fun executeTool(project: Project, tool: Tool, context: DataContext): Boolean {
    try {
      if (tool.isUseConsole) {
        withContext(Dispatchers.EDT) {
          val environment = create(project,
                                   DefaultRunExecutor.getRunExecutorInstance(),
                                   ToolRunProfile(tool, ToolAction.getToolDataContext(context)))
            .build()
          if (environment.state == null) {
            return@withContext false
          }

          environment.executionId = 0L
          environment.runner.execute(environment)
        }
      }
      else {
        val commandLine: GeneralCommandLine? = tool.createCommandLine(context)
        if (commandLine == null) {
          return false
        }
        val handler = OSProcessHandler(commandLine)
        handler.addProcessListener(ToolProcessAdapter(project, tool.synchronizeAfterExecution(), tool.name))
        handler.startNotify()
      }
    }
    catch (ex: ExecutionException) {
      ExecutionErrorDialog.show(ex, ToolsBundle.message("tools.process.start.error"), project)
      return false
    }
    return true
  }
}