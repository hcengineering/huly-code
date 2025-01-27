// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven.actions

import com.hulylabs.intellij.plugins.completion.providers.supermaven.SupermavenService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PopupAction

class ToggleGitignoreAction(private val supermaven: SupermavenService, private val gitignore: Boolean) : AnAction(), PopupAction {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    supermaven.setGitignoreAllowed(!gitignore)
  }

  override fun update(e: AnActionEvent) {
    val enabledText = if (gitignore) "Disable" else "Enable"
    e.presentation.text = "${enabledText} Supermaven in files ignored by .gitignore"
  }
}