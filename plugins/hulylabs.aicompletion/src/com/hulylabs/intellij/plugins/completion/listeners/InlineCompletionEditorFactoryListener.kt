// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.listeners

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

class InlineCompletionEditorFactoryListener : EditorFactoryListener {
  private val caretListener = InlineCompletionCaretListener()

  override fun editorCreated(event: EditorFactoryEvent) {
    val editor = event.editor
    editor.caretModel.addCaretListener(caretListener)
  }

  override fun editorReleased(event: EditorFactoryEvent) {
    val editor = event.editor
    editor.caretModel.removeCaretListener(caretListener)
  }
}