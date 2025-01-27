// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.intellij.util.messages.Topic
import com.intellij.util.messages.Topic.ProjectLevel


interface CompletionProviderStateChangedListener {
  fun stateChanged()

  companion object {
    @ProjectLevel
    val COMPLETION_PROVIDER_STATE_CHANGED: Topic<CompletionProviderStateChangedListener> = Topic.create("CompletionProviderStateChanged", CompletionProviderStateChangedListener::class.java)
  }
}