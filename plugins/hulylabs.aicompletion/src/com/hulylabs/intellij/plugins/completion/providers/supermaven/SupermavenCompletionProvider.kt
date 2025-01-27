// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.InlineCompletionProviderService
import com.hulylabs.intellij.plugins.completion.providers.supermaven.actions.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

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

  override fun getStatus(): String {
    return when (supermaven.state) {
      SupermavenService.AgentState.STARTING -> {
        when (supermaven.getAccountStatus()) {
          SupermavenAccountStatus.NEEDS_ACTIVATION -> "Needs activation"
          else -> "Starting"
        }
      }
      SupermavenService.AgentState.FAILED_DOWNLOAD -> "Failed to download agent"
      SupermavenService.AgentState.STARTED -> {
        when (supermaven.getAccountStatus()) {
          SupermavenAccountStatus.NEEDS_ACTIVATION -> "Needs activation"
          SupermavenAccountStatus.READY -> {
            supermaven.getServiceTier() ?: "Unknown"
          }
          else -> "Unknown"
        }
      }
      SupermavenService.AgentState.ERROR -> "Error"
      SupermavenService.AgentState.STOPPED -> "Stopped"
    }
  }

  override fun getActions(file: VirtualFile?): List<AnAction> {
    val actions: MutableList<AnAction> = mutableListOf()
    if (supermaven.getAccountStatus() == SupermavenAccountStatus.NEEDS_ACTIVATION) {
      actions.add(FreeActivationAction(supermaven))
      actions.add(ProActivationAction(supermaven))
    }
    else if (supermaven.state == SupermavenService.AgentState.STARTED) {
      if (supermaven.getServiceTier() == "FreeNoLicense") {
        actions.add(UpgradeProAction(supermaven))
      }
      val settings = ApplicationManager.getApplication().service<SupermavenSettings>()
      if (file != null && file.extension != null) {
        val extension = file.extension!!
        actions.add(EnableAction(extension))
      }
      actions.add(ToggleGitignoreAction(supermaven, settings.state.gitignoreAllowed))
      actions.add(LogoutAction(supermaven))
    }
    return actions
  }

  fun isFileSupported(file: VirtualFile): Boolean {
    return file.extension != null && !ApplicationManager.getApplication().service<SupermavenSettings>().state.disabledExtensions.contains(file.extension)
  }

  override fun update(file: VirtualFile, content: String, entryId: Int, cursorOffset: Int) {
    if (!isFileSupported(file)) {
      return
    }
    supermaven.update(file.path, content, entryId, cursorOffset)
  }

  override fun suggest(file: VirtualFile, content: String, entryId: Int, cursorOffset: Int): String? {
    if (!isFileSupported(file)) {
      return null
    }
    return supermaven.completion(content, entryId, cursorOffset)
  }
}