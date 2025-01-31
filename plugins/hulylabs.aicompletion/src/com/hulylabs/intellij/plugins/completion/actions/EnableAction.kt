// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.actions

import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class EnableAction(private val project: Project) : AnAction(), PopupAction {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val settings = ApplicationManager.getApplication().service<CompletionSettings>()
    settings.state.completionEnabled = !settings.state.completionEnabled
    val provider = InlineCompletionProviderRegistry.getProvider(project)
    if (settings.state.completionEnabled) {
      provider.start()
    }
    else {
      provider.stop()
    }
  }

  override fun update(e: AnActionEvent) {
    val settings = ApplicationManager.getApplication().service<CompletionSettings>()
    val enabledText = if (settings.state.completionEnabled) "Disable" else "Enable"
    e.presentation.text = "${enabledText} completion"
  }
}