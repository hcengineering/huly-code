// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.settings

import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.hulylabs.intellij.plugins.chat.providers.LanguageModelProviderRegistry
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(name = "HulyChat", storages = [Storage("HulyChat.xml")])
class ChatSettings : SimplePersistentStateComponent<SettingsState>(SettingsState()) {
  var activeLanguageModel: LanguageModel?
    get() {
      val id = state.activeLanguageModelId ?: return null
      val (providerId, modelId) = id.split(':')
      val provider = LanguageModelProviderRegistry.getInstance().getProvider(providerId)
      return provider?.providedModels()?.find { it.id == modelId }
    }
    set(value) {
      state.activeLanguageModelId = value?.let { "${value.provider.id}:${value.id}" }
    }

  companion object {
    @JvmStatic
    fun getInstance(): ChatSettings = service()
  }
}

class SettingsState : BaseState() {
  var activeLanguageModelId by string(null)
  var lmsBaseUrl by string(null)
}
