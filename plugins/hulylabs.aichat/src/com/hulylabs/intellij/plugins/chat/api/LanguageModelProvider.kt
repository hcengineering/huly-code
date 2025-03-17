// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.api

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.CompletableFuture

interface LanguageModelProvider {
  val id: String
  val name: String
  val enabled: Boolean
  val authenticated: Boolean

  fun authenticate(): CompletableFuture<Unit>
  fun providedModels(): List<LanguageModel>
  fun loadModel(model: LanguageModel) {}
  fun getTokenCount(request: List<ChatMessage>): Int
  suspend fun sendChatRequest(model: LanguageModel,request: List<ChatMessage>): Flow<ChatMessage>

  fun createSettingsPanel(): SettingsPanel
  fun cancelProcessing()
}