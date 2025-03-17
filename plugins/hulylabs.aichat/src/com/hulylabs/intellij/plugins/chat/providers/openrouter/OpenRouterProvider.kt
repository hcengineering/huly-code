// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.openrouter

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.hulylabs.intellij.plugins.chat.api.LanguageModelProvider
import com.hulylabs.intellij.plugins.chat.api.SettingsPanel
import com.hulylabs.intellij.plugins.chat.settings.ChatSettings
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.CompletableFuture

class OpenRouterProvider : LanguageModelProvider {
  override val id: String = "openrouter"
  override val name: String = "OpenRouter"
  override val enabled: Boolean = true

  override val authenticated: Boolean
    get() {
      return true
    }

  override fun getTokenCount(request: List<ChatMessage>): Int {
    return OpenRouterService.getInstance().getTokenCount()
  }

  override fun authenticate(): CompletableFuture<Unit> {
    return CompletableFuture.completedFuture(Unit)
  }

  override suspend fun sendChatRequest(model: LanguageModel, request: List<ChatMessage>): Flow<ChatMessage> {
    return OpenRouterService.getInstance().sendChatRequest(model, request)
  }

  override fun cancelProcessing() {
    OpenRouterService.getInstance().cancelProcessing()
  }

  override fun loadModel(model: LanguageModel) {
    // not used
  }

  override fun providedModels(): List<LanguageModel> {
    val models = ChatSettings.getInstance().state.openRoutersModels
    val routerModels = OpenRouterService.getInstance().getModels()
    return routerModels.filter { it.id in models }
      .map { LanguageModel(this, it.id, it.name, it.contextLength, true) }
  }

  override fun createSettingsPanel(): SettingsPanel {
    return OpenRouterSettingsPanel(this)
  }
}