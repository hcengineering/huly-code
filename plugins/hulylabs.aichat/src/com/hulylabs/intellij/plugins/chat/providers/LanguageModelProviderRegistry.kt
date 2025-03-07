// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers

import com.hulylabs.intellij.plugins.chat.api.LanguageModelProvider
import com.hulylabs.intellij.plugins.chat.providers.copilot.CopilotProvider
import com.hulylabs.intellij.plugins.chat.providers.lmstudio.LMStudioProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class LanguageModelProviderRegistry() {
  private val providers = mutableListOf<LanguageModelProvider>()

  init {
    providers.add(LMStudioProvider())
    providers.add(CopilotProvider())
  }

  fun getProviders(): List<LanguageModelProvider> {
    return providers
  }

  fun getProvider(id: String): LanguageModelProvider? {
    return providers.find { it.id == id }
  }

  companion object {
    @JvmStatic
    fun getInstance(): LanguageModelProviderRegistry = service()
  }
}