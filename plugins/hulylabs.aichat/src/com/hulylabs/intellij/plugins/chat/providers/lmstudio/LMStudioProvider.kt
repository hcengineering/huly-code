// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.lmstudio

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.hulylabs.intellij.plugins.chat.api.LanguageModelProvider
import com.hulylabs.intellij.plugins.chat.api.SettingsPanel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.*
import java.util.concurrent.CompletableFuture

class LMStudioProvider : LanguageModelProvider {
  override val id: String = "lmstudio"
  override val name: String = "LM Studio"
  override val enabled: Boolean = true

  private val availableModels = Collections.synchronizedList(mutableListOf<LanguageModel>())
  private val scope = MainScope().plus(CoroutineName("LMStudio"))

  override val authenticated: Boolean
    get() {
      return availableModels.isNotEmpty()
    }

  init {
    try {
      // try to authenticate on startup without error reporting
      authenticate()
    }
    catch (_: Exception) {
      // ignore
    }
  }

  override fun authenticate(): CompletableFuture<Unit> {
    val result = CompletableFuture<Unit>()
    if (availableModels.isEmpty()) {
      scope.launch(Dispatchers.IO) {
        try {
          fetchModels()
          result.complete(Unit)
        }
        catch (e: Exception) {
          result.completeExceptionally(e)
        }
      }
    }
    else {
      result.complete(Unit)
    }
    return result
  }

  override fun providedModels(): List<LanguageModel> {
    return availableModels
  }

  override fun loadModel(model: LanguageModel) {
    LMStudioService.loadModel(model.id)
  }

  override fun getTokenCount(request: List<ChatMessage>): Int {
    return request.sumOf { it.content.count { it.isWhitespace() } + 1 }
  }

  override suspend fun sendChatRequest(model: LanguageModel, request: List<ChatMessage>): Flow<ChatMessage> {
    return LMStudioService.sendChatRequest(model.id, request)
  }

  override fun createSettingsPanel(): SettingsPanel {
    return LMStudioSettingsPanel(this)
  }

  fun fetchModels(apiUrl: String? = null) {
    val models = LMStudioService.getModels(apiUrl).map { LanguageModel(this@LMStudioProvider, it.id, it.id.take(30), 0) }.sortedBy { it.id }
    availableModels.clear()
    availableModels.addAll(models)
  }
}