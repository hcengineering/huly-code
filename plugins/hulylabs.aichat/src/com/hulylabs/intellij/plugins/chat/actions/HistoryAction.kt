// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.actions

import com.hulylabs.intellij.plugins.chat.settings.ChatConversationInfo
import com.hulylabs.intellij.plugins.chat.settings.ChatHistory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.util.IconLoader
import kotlin.time.Duration.Companion.milliseconds

fun formatDuration(startTimeMillis: Long): String {
  val duration = (System.currentTimeMillis() - startTimeMillis).milliseconds
  return duration.toComponents { days, hours, minutes, seconds, _ ->
    when {
      days > 0 -> "${days} days"
      hours > 0 -> "${hours} hours"
      minutes > 0 -> "${minutes} minutes"
      else -> "${seconds} seconds"
    }
  }
}

class HistoryItemAction(
  private val conversation: ChatConversationInfo,
  private val onPerform: (conversationId: Long) -> Unit,
) : AnAction("${conversation.name} : ${formatDuration(conversation.lastUpdated)}") {
  override fun actionPerformed(e: AnActionEvent) {
    onPerform(conversation.created)
  }
}

class HistoryAction(private val onPerform: (conversationId: Long) -> Unit)
  : DefaultActionGroup("History", null, IconLoader.getIcon("/icons/history.svg", HistoryAction::class.java)) {
  init {
    templatePresentation.isPopupGroup = true
  }

  override fun getChildren(e: AnActionEvent?): Array<out AnAction?> {
    val history = ChatHistory.getInstance().history()
    return history.map { HistoryItemAction(it, onPerform) }.toTypedArray()
  }
}