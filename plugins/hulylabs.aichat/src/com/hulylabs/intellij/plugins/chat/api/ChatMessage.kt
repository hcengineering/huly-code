// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.api

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
  var content: String,
  var role: String,
  var id: String? = null,
  var isError: Boolean = false,
)