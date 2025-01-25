// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
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
        return JLabel(if (project != null) InlineCompletionProviderRegistry.getInstance(project).provider.name else "Inline Completion")
      }
    }

  override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
    return InlineCompletionSingleSuggestion.build {
      withContext(Dispatchers.EDT) {
        getCompletionElement(request.editor)
      }?.let {
        emit(it)
      }
    }
  }

  private fun getCompletionElement(editor: Editor): InlineCompletionElement? {
    val provider = InlineCompletionProviderRegistry.getInstance(editor.project!!).provider
    val completion = provider.suggest(editor.document.text, editor.document.hashCode(), editor.caretModel.offset)
    return completion?.let {
      InlineCompletionGrayTextElement(completion)
    }
  }

  override fun isEnabled(event: InlineCompletionEvent): Boolean {
    println("event: ${event.javaClass.simpleName}")
    return true
  }
}