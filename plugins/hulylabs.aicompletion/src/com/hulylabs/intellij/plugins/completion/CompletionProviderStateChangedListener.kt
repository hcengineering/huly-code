// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.intellij.util.messages.Topic
import com.intellij.util.messages.Topic.ProjectLevel
import java.util.*


interface CompletionProviderStateChangedListener : EventListener {
  fun stateChanged()

  companion object {
    @ProjectLevel
    @JvmField
    val TOPIC: Topic<CompletionProviderStateChangedListener> = Topic(CompletionProviderStateChangedListener::class.java, Topic.BroadcastDirection.NONE)
  }
}