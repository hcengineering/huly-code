// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.InlineCompletionProviderService
import com.hulylabs.intellij.plugins.completion.providers.supermaven.actions.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.max

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
            supermaven.getServiceTier() ?: "Starting"
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

  private fun isFileSupported(file: VirtualFile): Boolean {
    return file.extension != null && !ApplicationManager.getApplication().service<SupermavenSettings>().state.disabledExtensions.contains(file.extension)
  }

  override fun update(file: VirtualFile, content: String, entryId: Int, cursorOffset: Int) {
    if (!isFileSupported(file)) {
      return
    }
    supermaven.update(file.path, content, entryId, cursorOffset)
  }

  override suspend fun suggest(file: VirtualFile, content: String, entryId: Int, cursorOffset: Int): Flow<String>? {
    if (!isFileSupported(file)) {
      return null
    }
    return flow {
      delay(20)
      val stateId = supermaven.completion(content, entryId, cursorOffset) ?: return@flow
      val now = System.currentTimeMillis()
      var i = 0
      while (System.currentTimeMillis() - now < 10000) {
        val state = supermaven.completionState(stateId) ?: break
        while (i < state.chunks.size) {
          if (i == 0) {
            var idxOffset = max(0, cursorOffset - 20)
            var pattern = content.substring(idxOffset, cursorOffset)
            while (idxOffset < cursorOffset && pattern.isNotEmpty()) {
              if (state.chunks[i].startsWith(pattern)) {
                break
              }
              pattern = pattern.substring(1)
              idxOffset++
            }
            if (pattern.isNotEmpty()) {
              emit(state.chunks[i].substring(pattern.length))
            }
            else {
              emit(state.chunks[i])
            }
          }
          else if (state.end && i == state.chunks.size - 1) {
            emit(state.chunks[i].trimEnd())
          }
          else {
            emit(state.chunks[i])
          }
          i++
        }
        if (state.end) break
        delay(50)
      }
    }
  }
}