// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat

import com.intellij.toolWindow.DefaultToolWindowLayoutBuilder
import com.intellij.toolWindow.DefaultToolWindowLayoutExtension

class ChatToolWindowLayoutPlatformExtension : DefaultToolWindowLayoutExtension {
  override fun buildV1Layout(builder: DefaultToolWindowLayoutBuilder) {}

  override fun buildV2Layout(builder: DefaultToolWindowLayoutBuilder) {
    builder.right.addOrUpdate("AI Chat") { weight = 0.1f }
  }
}