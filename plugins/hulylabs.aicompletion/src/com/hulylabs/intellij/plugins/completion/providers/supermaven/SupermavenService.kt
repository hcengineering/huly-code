// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.*
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val LOG = logger<SupermavenService>()

@Service(Service.Level.PROJECT)
class SupermavenService(val project: Project, val scope: CoroutineScope) : Disposable {
  enum class AgentState {
    STARTING, FAILED_DOWNLOAD, STARTED, ERROR, STOPPED
  }

  private var agent: SupermavenAgent? = null
  var state: AgentState = AgentState.STOPPED
    private set

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
        agent = SupermavenAgent(project, agentPath)
        state = AgentState.STARTED
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
    if (agent!!.accountStatus == SupermavenAccountStatus.NEEDS_ACTIVATION) {
      // on activation state we send update command for communication
      return true
    }
    LOG.warn("Supermaven agent account status is ${agent!!.accountStatus}")
    return false
  }

  fun update(path: String, content: String, cursorOffset: Int) {
    if (!checkStarted()) {
      return
    }
    agent!!.newCompletionState(path, cursorOffset)
    val msg = SupermavenStateUpdateMessage(agent!!.newStateId.toString(), listOf(
      FileUpdateMessage(path, content),
      CursorPositionUpdateMessage(path, cursorOffset)
    ))
    agent!!.send(msg)
  }

  fun completion(path: String, content: String, cursorOffset: Int): Long? {
    if (!checkStarted()) {
      return null
    }
    agent?.drainOutput()
    var stateId = agent!!.newStateId
    while (agent!!.states.containsKey(stateId)) {
      val state = agent!!.states[stateId]!!
      if (path == state.path && state.prefixOffset == cursorOffset) {
        return stateId
      }
      stateId--
    }
    return null
  }

  fun completionState(stateId: Long): SupermavenCompletionState? {
    return agent?.states?.get(stateId)
  }

  fun getAccountStatus(): SupermavenAccountStatus? {
    return agent?.accountStatus
  }

  fun getServiceTier(): String? {
    return agent?.serviceTier
  }

  fun setGitignoreAllowed(allowed: Boolean) {
    val settings = ApplicationManager.getApplication().service<SupermavenSettings>()
    settings.state.gitignoreAllowed = allowed
    if (!checkStarted()) {
      return
    }
    agent!!.send(SupermavenGreetingsMessage(allowed))
  }

  fun signIn(free: Boolean) {
    if (agent?.accountStatus == SupermavenAccountStatus.NEEDS_ACTIVATION) {
      if (free) {
        agent!!.send(SupermavenFreeActivationMessage())
      }
      else if (agent!!.activationUrl != null) {
        BrowserUtil.open(agent!!.activationUrl!!)
      }
    }
  }

  fun logout() {
    if (!checkStarted()) {
      return
    }
    agent!!.serviceTier = null
    agent!!.send(SupermavenLogoutMessage)
  }

  override fun dispose() {
    stop()
  }
}