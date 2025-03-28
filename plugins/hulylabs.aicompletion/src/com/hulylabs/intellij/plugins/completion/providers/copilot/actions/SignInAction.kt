// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot.actions

import com.hulylabs.intellij.plugins.completion.providers.copilot.CopilotService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PopupAction

class SignInAction(private val copilot: CopilotService) : AnAction(), PopupAction {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    copilot.signIn()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.text = "SignIn"
  }
}