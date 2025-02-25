// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers

import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionSkipTextElement
import junit.framework.TestCase.assertEquals
import org.junit.Test

class CompletionUtilsTest {
  fun assertItem(elem: InlineCompletionElement, expected: String, isText: Boolean = true) {
    assertEquals(expected, elem.text)
    if (isText) {
      assertEquals(InlineCompletionGrayTextElement::class, elem::class)
    } else {
      assertEquals(InlineCompletionSkipTextElement::class, elem::class)
    }
  }

  @Test
  fun testSplitCompletion() {
    val txt = "Doc>(idest: Ref<T>): T[] {"
    val lineSuffix = ">() {"
    val result = CompletionUtils.splitCompletion(txt, lineSuffix)
    assertEquals(8, result.size)
    assertItem(result[0], "Doc")
    assertItem(result[1], ">(", false)
    assertItem(result[2], "idest: Ref<T>")
    assertItem(result[3], ")", false)
    assertItem(result[4], ":")
    assertItem(result[5], " ", false)
    assertItem(result[6], "T[] ")
    assertItem(result[7], "{", false)
  }

  @Test
  fun testSplitCompletion2() {
    val txt = "ame\": \"Person Name\""
    val lineSuffix = "\""
    val result = CompletionUtils.splitCompletion(txt, lineSuffix)
    assertEquals(3, result.size)
    assertItem(result[0], "ame")
    assertItem(result[1], "\"", false)
    assertItem(result[2], ": \"Person Name\"")
  }
}