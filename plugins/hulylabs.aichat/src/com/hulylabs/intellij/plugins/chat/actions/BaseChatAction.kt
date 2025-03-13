// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.IconLoader

abstract class BaseChatAction(name: String, icon: String)
  : AnAction(name, null, IconLoader.getIcon("/icons/${icon}", BaseChatAction::class.java)) {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = true
  }
}