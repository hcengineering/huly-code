// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.hulylabs.intellij.plugins.completion.providers.InlineCompletionProviderService
import com.hulylabs.intellij.plugins.completion.providers.supermaven.SupermavenCompletionProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class InlineCompletionProviderRegistry(project: Project) {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): InlineCompletionProviderRegistry = project.service()
  }

  val provider: InlineCompletionProviderService = SupermavenCompletionProvider(project)

  init {
    provider.start()
  }
}