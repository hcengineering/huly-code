// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.ResponseItem
import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.ResponseItemKind
import kotlin.math.min

class SupermavenCompletionState(val path: String, val prefix: String) {
  var completion: List<ResponseItem> = listOf()
  var end: Boolean = false

  fun stripPrefix(originalPrefix: String): List<ResponseItem> {
    var prefix = originalPrefix
    val remainingResponseItems = mutableListOf<ResponseItem>()
    for (item in completion) {
      when (item.kind) {
        ResponseItemKind.TEXT -> {
          var text = item.text!!
          if (!sharesCommonPrefix(text, prefix)) {
            return emptyList()
          }
          val trimLength = min(text.length, prefix.length)
          text = text.substring(trimLength)
          prefix = prefix.substring(trimLength)
          if (text.isNotEmpty()) {
            remainingResponseItems.add(ResponseItem(ResponseItemKind.TEXT, text))
          }
        }
        ResponseItemKind.DELETE -> {
          remainingResponseItems.add(item)
        }
        ResponseItemKind.DEDENT -> {
          if (prefix.isNotEmpty()) {
            return emptyList()
          }
          remainingResponseItems.add(item)
        }
        else -> {
          if (prefix.isEmpty()) {
            remainingResponseItems.add(item)
          }
        }
      }
    }
    return remainingResponseItems
  }

  private fun sharesCommonPrefix(str1: String, str2: String): Boolean {
    val minLength = min(str1.length, str2.length)
    for (i in 0 until minLength) {
      if (str1[i] != str2[i]) {
        return false
      }
    }
    return true
  }
}