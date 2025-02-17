// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers

import junit.framework.TestCase.assertEquals
import org.junit.Test

class CompletionUtilsTest {
  @Test
  fun testSplitCompletion() {
    val txt = "Doc>(idest: Ref<T>): T[] {"
    val lineSuffix = ">() {"
    val result = CompletionUtils.splitCompletion(txt, lineSuffix)
    assertEquals(8, result.size)
    assertEquals("Doc", result[0].text)
    assertEquals(">(", result[1].text)
    assertEquals("idest: Ref<T>", result[2].text)
    assertEquals(")", result[3].text)
    assertEquals(":", result[4].text)
    assertEquals(" ", result[5].text)
    assertEquals("T[] ", result[6].text)
    assertEquals("{", result[7].text)
  }
}