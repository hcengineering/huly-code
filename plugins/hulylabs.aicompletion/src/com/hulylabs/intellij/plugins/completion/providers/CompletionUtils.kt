// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers

import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionSkipTextElement

object CompletionUtils {
  @JvmStatic
  fun splitCompletion(completion: String, lineSuffix: String): List<InlineCompletionElement> {
    var idx = 0
    var suffixIdx = 0
    var result = mutableListOf<InlineCompletionElement>()
    var suffix = ""
    var txt = ""
    while (idx < completion.length && suffixIdx < lineSuffix.length) {
      if (completion[idx] == lineSuffix[suffixIdx]) {
        if (txt.isNotEmpty()) {
          result.add(InlineCompletionGrayTextElement(txt))
          txt = ""
        }
        suffix += lineSuffix[suffixIdx]
        suffixIdx++
      } else {
        if (suffix.isNotEmpty()) {
          result.add(InlineCompletionSkipTextElement(suffix))
          suffix = ""
        }
        txt += completion[idx]
      }
      idx++
    }
    if (idx < completion.length) {
      txt += completion.substring(idx)
    }
    if (suffix.isNotEmpty()) {
      result.add(InlineCompletionSkipTextElement(suffix))
    }
    if (txt.isNotEmpty()) {
      result.add(InlineCompletionGrayTextElement(txt))
    }
    return result
  }
}