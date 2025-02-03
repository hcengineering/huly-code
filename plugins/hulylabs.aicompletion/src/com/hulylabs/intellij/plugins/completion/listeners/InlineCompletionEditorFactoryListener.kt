// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.listeners

import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.intellij.codeInsight.inline.completion.InlineCompletion
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

class InlineCompletionEditorFactoryListener : EditorFactoryListener {
  private val caretListener = InlineCompletionCaretListener()
  private val completionListenersMap = mutableMapOf<Editor, InlineCompletionEventListener>()
  private val documentListenersMap = mutableMapOf<Editor, InlineCompletionDocumentListener>()

  override fun editorCreated(event: EditorFactoryEvent) {
    val editor = event.editor
    if (editor.virtualFile == null) {
      return
    }
    editor.caretModel.addCaretListener(caretListener)
    val documentListener = InlineCompletionDocumentListener(editor)
    documentListenersMap[editor] = documentListener
    editor.document.addDocumentListener(documentListener)
    val handler = InlineCompletion.getHandlerOrNull(editor)
    if (handler != null) {
      val completionListener = InlineCompletionEventListener(editor)
      handler.addEventListener(completionListener)
      completionListenersMap[editor] = completionListener
    }
    if (ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(event.editor.virtualFile)) {
      val provider = InlineCompletionProviderRegistry.getProvider(editor.project!!)
      provider.documentOpened(editor.virtualFile!!, editor.document.text)
    }
  }

  override fun editorReleased(event: EditorFactoryEvent) {
    val editor = event.editor
    if (editor.virtualFile == null) {
      return
    }
    editor.caretModel.removeCaretListener(caretListener)
    documentListenersMap.remove(editor)?.let {
      editor.document.removeDocumentListener(it)
    }
    val handler = InlineCompletion.getHandlerOrNull(editor)
    val completionListener = completionListenersMap.remove(editor)
    if (handler != null && completionListener != null) {
      handler.removeEventListener(completionListener)
    }
    if (ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(event.editor.virtualFile)) {
      val provider = InlineCompletionProviderRegistry.getProvider(editor.project!!)
      provider.documentClosed(editor.virtualFile!!)
    }
  }
}