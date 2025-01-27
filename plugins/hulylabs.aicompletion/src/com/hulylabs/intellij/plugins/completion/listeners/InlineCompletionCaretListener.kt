// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.listeners

import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.hulylabs.intellij.plugins.completion.ui.WidgetRegistry
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener

class InlineCompletionCaretListener : CaretListener {
  override fun caretPositionChanged(event: CaretEvent) {
    if (event.editor.virtualFile != null && event.caret != null && event.editor.project != null) {
      if (event.editor.document.textLength < 1_000_000) {
        val content = event.editor.document.text
        val entryId = event.editor.document.hashCode()
        val provider = InlineCompletionProviderRegistry.getInstance(event.editor.project!!).provider
        provider.update(event.editor.virtualFile!!, content, entryId, event.caret!!.offset)
      }
    }
    WidgetRegistry.getWidgets().forEach {
      it.update()
    }
  }
}

