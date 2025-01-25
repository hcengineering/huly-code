// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.InlineCompletionProviderService
import com.hulylabs.intellij.plugins.completion.providers.supermaven.actions.LogoutAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

class SupermavenCompletionProvider(val project: Project) : InlineCompletionProviderService {
  private val supermaven: SupermavenService = SupermavenService.getInstance(project)
  override val name: String
    get() = "Supermaven"

  override fun start() {
    supermaven.start()
  }

  override fun stop() {
    supermaven.stop()
  }

  override fun getActions(): List<AnAction> {
    val actions: MutableList<AnAction> = mutableListOf()
    actions.add(LogoutAction(supermaven))
    return actions
  }

  override fun isUpdating(): Boolean {
    TODO("Not yet implemented")
  }

  override fun update(path: String, content: String, entryId: Int, cursorOffset: Int) {
    supermaven.update(path, content, entryId, cursorOffset)
  }

  override fun suggest(content: String, entryId: Int, cursorOffset: Int): String? {
    return supermaven.completion(content, entryId, cursorOffset)
  }
}