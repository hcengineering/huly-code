// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.api

data class LanguageModel(
  val provider: LanguageModelProvider,
  val id: String,
  val displayName: String,
  val maxContextLength: Int,
  val useStreaming: Boolean = true,
)