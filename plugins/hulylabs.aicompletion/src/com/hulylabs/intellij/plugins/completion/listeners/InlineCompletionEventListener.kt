// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.listeners

import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.intellij.codeInsight.inline.completion.InlineCompletionEventAdapter
import com.intellij.codeInsight.inline.completion.InlineCompletionEventType
import com.intellij.codeInsight.inline.completion.logs.InlineCompletionUsageTracker
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor

class InlineCompletionEventListener(private val editor: Editor) : InlineCompletionEventAdapter {
  override fun onHide(event: InlineCompletionEventType.Hide) {
    when (event.finishType) {
      InlineCompletionUsageTracker.ShownEvents.FinishType.SELECTED -> notifyAccepted()
      InlineCompletionUsageTracker.ShownEvents.FinishType.ESCAPE_PRESSED -> notifyRejected()
      else -> {}
    }
  }

  private fun notifyAccepted() {
    if (ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(editor.virtualFile)) {
      InlineCompletionProviderRegistry.getProvider(editor.project!!).completionAccepted()
    }
  }

  private fun notifyRejected() {
    if (ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(editor.virtualFile)) {
      InlineCompletionProviderRegistry.getProvider(editor.project!!).completionRejected()
    }
  }
}