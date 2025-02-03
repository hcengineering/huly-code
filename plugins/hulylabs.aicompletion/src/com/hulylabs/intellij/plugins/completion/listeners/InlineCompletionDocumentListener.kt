// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.listeners

import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

class InlineCompletionDocumentListener(private val editor: Editor) : DocumentListener {
  override fun documentChanged(event: DocumentEvent) {
    if (ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(editor.virtualFile)) {
      val provider = InlineCompletionProviderRegistry.getProvider(editor.project!!)
      provider.documentChanged(editor.virtualFile, event)
    }
  }
}