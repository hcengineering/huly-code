// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.CursorPositionUpdateMessage
import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.FileUpdateMessage
import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.SupermavenLogoutMessage
import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.SupermavenStateUpdateMessage
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val LOG = logger<SupermavenService>()

@Service(Service.Level.PROJECT)
class SupermavenService(val project: Project, val scope: CoroutineScope) {
  enum class AgentState {
    STARTING, FAILED_DOWNLOAD, STARTED, ERROR, STOPPED
  }

  private var agent: SupermavenAgent? = null
  private var state: AgentState = AgentState.STOPPED

  companion object {
    @JvmStatic
    fun getInstance(project: Project): SupermavenService = project.service()
  }

  fun start() {
    if (agent == null && state == AgentState.STOPPED) {
      state = AgentState.STARTING
      scope.launch {
        val agentPath =
          try {
            SupermavenRuntime().getAgentPath(project)
          }
          catch (e: Exception) {
            LOG.error("failed to get agent path", e)
            state = AgentState.FAILED_DOWNLOAD
            return@launch
          }
        agent = SupermavenAgent(agentPath)
      }
    }
  }

  fun stop() {
    if (agent != null) {
      agent!!.destroy()
      agent = null
      state = AgentState.STOPPED
    }
  }

  private fun checkStarted(): Boolean {
    if (agent == null) {
      LOG.warn("Supermaven agent not started")
      return false
    }
    if (!agent!!.isAlive()) {
      LOG.warn("Supermaven agent stopped")
      state = AgentState.STOPPED
      return false
    }
    if (agent!!.accountStatus == SupermavenAccountStatus.READY) {
      state = AgentState.STARTED
      return true
    }

    LOG.warn("Supermaven agent account status is ${agent!!.accountStatus}")
    return false
  }

  fun update(path: String, content: String, entryId: Int, cursorOffset: Int) {
    if (!checkStarted()) {
      return
    }

    agent!!.newCompletionState(entryId, cursorOffset)
    val msg = SupermavenStateUpdateMessage(agent!!.newStateId.toString(), listOf(
      FileUpdateMessage(path, content),
      CursorPositionUpdateMessage(path, cursorOffset)
    ))
    agent!!.send(msg)
  }

  fun completion(content: String, entryId: Int, cursorOffset: Int): String? {
    if (!checkStarted()) {
      return null
    }
    var bestCompletion: String? = null
    agent?.drainOutput()
    for (state in agent!!.states.values) {
      // ignore state if content was modified
      if (entryId != state.entryId) {
        continue
      }
      if (!state.text.startsWith(state.dedent)) {
        //LOG.warn("state text does not start with dedent, '${state.text}', '${state.dedent}'")
        continue
      }
      var stateCompletion = if (state.dedent.isNotEmpty()) state.text.substring(state.dedent.length) else state.text
      if (state.prefixOffset < cursorOffset) {
        val textInsertedSinceCompletionRequest = content.substring(state.prefixOffset..<cursorOffset)
        //LOG.info("stateCompletion: $textInsertedSinceCompletionRequest")
        if (textInsertedSinceCompletionRequest.isNotEmpty()) {
          if (state.text.startsWith(textInsertedSinceCompletionRequest)) {
            stateCompletion = state.text.substring(textInsertedSinceCompletionRequest.length)
          }
          else {
            continue
          }
        }
      }
      else if (state.prefixOffset == cursorOffset) {
        stateCompletion = state.text
      }
      else {
        continue
      }
      if (bestCompletion != null && bestCompletion.length > stateCompletion.length) {
        continue
      }
      bestCompletion = stateCompletion
    }
    return bestCompletion
  }

  fun logout() {
    if (!checkStarted()) {
      return
    }
    agent!!.send(SupermavenLogoutMessage())
  }
}