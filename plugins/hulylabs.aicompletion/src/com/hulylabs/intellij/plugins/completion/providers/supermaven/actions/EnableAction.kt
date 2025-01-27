// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven.actions

import com.hulylabs.intellij.plugins.completion.providers.supermaven.SupermavenSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service

class EnableAction(val extension: String) : AnAction(), PopupAction {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val settings = ApplicationManager.getApplication().service<SupermavenSettings>()
    if (settings.state.disabledExtensions.contains(extension)) {
      settings.state.disabledExtensions.remove(extension)
    }
    else {
      settings.state.disabledExtensions.add(extension)
    }
  }

  override fun update(e: AnActionEvent) {
    val settings = ApplicationManager.getApplication().service<SupermavenSettings>()
    val enabledText = if (settings.state.disabledExtensions.contains(extension)) "Enable" else "Disable"
    e.presentation.text = "${enabledText} for '*.$extension'"
  }
}