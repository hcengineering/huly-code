// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.inline.completion.elements

import com.intellij.codeInsight.inline.completion.InlineCompletionFontUtils
import com.intellij.openapi.editor.Editor

class InlineCompletionReplaceElement(displayText: String, val replaceText: String, val startOffset: Int, val endOffset: Int, val endLineOffset: Int)
  : InlineCompletionColorTextElement(displayText, InlineCompletionFontUtils::color) {
  override fun toPresentable(): InlineCompletionElement.Presentable = Presentable(this, endLineOffset)

  open class Presentable(
    element: InlineCompletionElement,
    val endLineOffset: Int,
  ) : InlineCompletionColorTextElement.Presentable(element, InlineCompletionFontUtils::color) {
    private var startOffset: Int? = null

    override fun startOffset(): Int? {
      return startOffset
    }

    override fun render(editor: Editor, offset: Int) {
      startOffset = offset
      super.render(editor, offset + endLineOffset)
    }
  }
}