// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.hulylabs.intellij.plugins.completion.providers.InlineCompletionProviderService
import com.hulylabs.intellij.plugins.completion.providers.copilot.CopilotCompletionProvider
import com.hulylabs.intellij.plugins.completion.providers.supermaven.SupermavenCompletionProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class InlineCompletionProviderRegistry(project: Project) : Disposable {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): InlineCompletionProviderRegistry = project.service()

    @JvmStatic
    fun getProvider(project: Project): InlineCompletionProviderService {
      return getInstance(project).provider
    }
  }

  private val providers: List<InlineCompletionProviderService> = listOf(SupermavenCompletionProvider(project), CopilotCompletionProvider(project))

  private var provider: InlineCompletionProviderService

  init {
    val settings = ApplicationManager.getApplication().service<CompletionSettings>()
    provider = providers[settings.state.activeProviderIdx]
    if (settings.isCompletionEnabled()) {
      provider.start()
    }
  }

  fun getProvidersNames(): List<String> {
    return providers.map { it.name }
  }

  fun setProvider(providerIdx: Int) {
    provider.stop()
    provider = providers[providerIdx]
    provider.start()
  }

  override fun dispose() {
    provider.stop()
  }
}