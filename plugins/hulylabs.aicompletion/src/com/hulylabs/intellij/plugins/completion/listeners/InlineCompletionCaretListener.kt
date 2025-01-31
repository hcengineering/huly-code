// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.listeners

import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener

class InlineCompletionCaretListener : CaretListener {
  override fun caretPositionChanged(event: CaretEvent) {
    if (event.editor.virtualFile != null && event.caret != null && event.editor.project != null
        && ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(event.editor.virtualFile)) {
      if (event.editor.document.textLength < 1_000_000) {
        val content = event.editor.document.text
        val entryId = event.editor.document.hashCode()
        val provider = InlineCompletionProviderRegistry.getProvider(event.editor.project!!)
        provider.update(event.editor.virtualFile!!, content, entryId, event.caret!!.offset)
      }
    }
  }
}

