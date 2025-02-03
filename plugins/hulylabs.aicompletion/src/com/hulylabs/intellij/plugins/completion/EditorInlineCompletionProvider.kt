// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent
import javax.swing.JLabel

class EditorInlineCompletionProvider : InlineCompletionProvider {
  override val id: InlineCompletionProviderID
    get() = InlineCompletionProviderID("AIInlineCompletionProvider")

  override val providerPresentation: InlineCompletionProviderPresentation
    get() = object : InlineCompletionProviderPresentation {
      override fun getTooltip(project: Project?): JComponent {
        return JLabel(if (project != null) InlineCompletionProviderRegistry.getProvider(project).name else "Inline Completion")
      }
    }

  override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
    if (!ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(request.editor.virtualFile)) {
      return InlineCompletionSuggestion.Empty
    }
    return InlineCompletionSingleSuggestion.build {
      val editor = request.editor
      val provider = InlineCompletionProviderRegistry.getProvider(editor.project!!)
      val caretOffset = withContext(Dispatchers.EDT) {
        editor.caretModel.offset
      }
      val flow = provider.suggest(editor.virtualFile, editor.document, caretOffset)
      flow?.collect {
        emit(InlineCompletionGrayTextElement(it))
      } ?: InlineCompletionSuggestion.Empty
    }
  }

  override fun isEnabled(event: InlineCompletionEvent): Boolean {
    if (!ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled()) {
      return false
    }
    return event is InlineCompletionEvent.DocumentChange || event is InlineCompletionEvent.Backspace
  }
}