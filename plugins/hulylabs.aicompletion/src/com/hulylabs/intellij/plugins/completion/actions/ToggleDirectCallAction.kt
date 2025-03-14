// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.actions

import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project

class ToggleDirectCallAction(private val project: Project) : ToggleAction(), PopupAction {
  init {
    templatePresentation.text = "Disable auto propose"
    val keys = KeymapManager.getInstance().getKeymap("CallInlineCompletionAction")
    templatePresentation.description = "It disables auto propose inline completion and only proposes direct calls by shortcut (${keys?.displayName})"
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return CompletionSettings.getInstance().state.onlyDirectCalls
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    CompletionSettings.getInstance().state.onlyDirectCalls = state
  }
}