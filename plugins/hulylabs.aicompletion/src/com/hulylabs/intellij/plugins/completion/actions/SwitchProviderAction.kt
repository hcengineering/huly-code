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

class SwitchProviderAction(private val project: Project) : AnAction(), PopupAction {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val settings = ApplicationManager.getApplication().service<CompletionSettings>()
    val registry = InlineCompletionProviderRegistry.getInstance(project)
    val providerNames = registry.getProvidersNames()
    settings.state.activeProviderIdx = (settings.state.activeProviderIdx + 1) % providerNames.size
    registry.setProvider(settings.state.activeProviderIdx)
  }

  override fun update(e: AnActionEvent) {
    val settings = ApplicationManager.getApplication().service<CompletionSettings>()
    val names = InlineCompletionProviderRegistry.getInstance(project).getProvidersNames()
    val providerName = names[(settings.state.activeProviderIdx + 1) % names.size]
    e.presentation.text = "Switch to ${providerName}"
  }
}