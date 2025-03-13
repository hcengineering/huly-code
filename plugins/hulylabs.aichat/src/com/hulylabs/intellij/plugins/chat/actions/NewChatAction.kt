// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.actions

import com.intellij.openapi.actionSystem.AnActionEvent

class NewChatAction(
  private val onAction: () -> Unit
) : BaseChatAction("New Chat", "add.svg") {
  override fun actionPerformed(e: AnActionEvent) {
    onAction()
  }
}