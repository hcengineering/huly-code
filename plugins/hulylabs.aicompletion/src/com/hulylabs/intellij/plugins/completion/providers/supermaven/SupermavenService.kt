// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.*
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val LOG = Logger.getInstance("#supermaven.service")

enum class CompletionType {
  TEXT, DEDENT, DELETE
}

data class CompletionItem(
  val type: CompletionType,
  val text: String,
  val dedent: String,
  val stateId: Long?,
  val completionIndex: Int?,
  val isComplete: Boolean,
)

data class CompletionParams(
  val lineBeforeCursor: String,
  val lineAfterCursor: String,
)

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

  fun update(path: String, content: String, cursor: Int) {
    if (!checkStarted()) {
      return
    }
    val prefix = content.substring(0, cursor)
    agent!!.newCompletionState(path, prefix)
    val msg = SupermavenStateUpdateMessage(agent!!.newStateId.toString(), listOf(
      FileUpdateMessage(path, content),
      CursorPositionUpdateMessage(path, cursor)
    ))
    agent!!.send(msg)
  }

  fun completion(path: String, content: String, cursor: Int, params: CompletionParams): CompletionItem? {
    if (!checkStarted()) {
      return null
    }
    agent?.drainOutput()
    val states = agent?.states ?: return null
    val prefix = content.substring(0, cursor)
    var bestLength = 0
    var bestStateId = 0L
    var bestCompletion = emptyList<ResponseItem>()
    for ((stateId, state) in states) {
      val statePrefix = state.prefix
      if (prefix.startsWith(statePrefix) && prefix.length >= statePrefix.length) {
        val userInput = prefix.substring(statePrefix.length)
        val remainingCompletion = state.stripPrefix(userInput)
        if (remainingCompletion.isNotEmpty()) {
          val totalLength = remainingCompletion.sumOf { it.text?.length ?: 0 }
          if (totalLength > bestLength || (totalLength == bestLength && stateId > bestStateId)) {
            bestCompletion = remainingCompletion
            bestLength = totalLength
            bestStateId = stateId
          }
        }
      }
    }
    return createCompletion(bestCompletion, bestStateId, params)
  }

  private fun isAllDust(str: String, dustStrings: List<String>): Boolean {
    var lineHolding = str
    while (lineHolding.isNotEmpty()) {
      val originalLength = lineHolding.length
      lineHolding = lineHolding.trimStart()
      for (dustString in dustStrings) {
        if (lineHolding.substring(0, dustString.length - 1) == dustString) {
          lineHolding = lineHolding.substring(dustString.length)
        }
      }
      if (lineHolding.length == originalLength) {
        return false
      }
    }
    return true
  }

  private fun canDelete(params: CompletionParams): Boolean {
    return !(params.lineBeforeCursor.trim().isEmpty() and (!isAllDust(params.lineAfterCursor.trim(), agent!!.dustStrings)))
  }

  private fun finishCompletion(output: String, dedent: String, stateId: Long?, idx: Int?, params: CompletionParams): CompletionItem? {
    if (!canDelete(params)) {
      return null
    }
    val hasTrailingCharacters = params.lineAfterCursor.trim().isNotEmpty()
    if (output.trim().isEmpty()) {
      return null
    }
    if (hasLeadingNewline(output)) {
      val firstNonEmptyLine = findFirstNonEmptyNewline(output)
      val lastNewLine = findLastNewline(output)
      if (firstNonEmptyLine != null && lastNewLine != null) {
        val text = output.substring(0, lastNewLine)
        return CompletionItem(CompletionType.TEXT, text, dedent, stateId, idx, true)
      }
      return null
    }
    else {
      val index = findFirstNonEmptyNewline(output)
      if (index != null) {
        val text = output.substring(0, index)
        return CompletionItem(CompletionType.TEXT, text, dedent, stateId, idx, true)
      }
    }
    if (hasTrailingCharacters || params.lineBeforeCursor.trim().isEmpty()) {
      return null
    }
    return CompletionItem(CompletionType.TEXT, output, dedent, stateId, idx, false)
  }

  private fun findLastNewline(str: String): Int? {
    for (i in str.indices.reversed()) {
      if (str[i] != '\n') {
        return i
      }
    }
    return null
  }

  private fun hasLeadingNewline(str: String): Boolean {
    for (i in str.indices) {
      if (str[i] == '\n') {
        return true
      }
      else if (!str[i].isWhitespace()) {
        return false
      }
    }
    return false
  }

  private fun findFirstNonEmptyNewline(str: String): Int? {
    var seenNonWhitespace = false
    for (i in str.indices) {
      if (str[i] == '\n' && seenNonWhitespace) {
        return i
      }
      else if (!str[i].isWhitespace()) {
        seenNonWhitespace = true
      }
    }
    return null
  }

  private fun createCompletion(completions: List<ResponseItem>, stateId: Long?, params: CompletionParams): CompletionItem? {
    if (completions.isEmpty()) {
      return null
    }
    var output = ""
    var dedent = ""

    for ((idx, item) in completions.withIndex()) {
      when (item.kind) {
        ResponseItemKind.END -> {
          if (output.contains('\n')) {
            return finishCompletion("$output\n", dedent, stateId, idx, params)
          }
          else {
            return null
          }
        }
        ResponseItemKind.TEXT -> {
          output += item.text!!
        }
        ResponseItemKind.DEDENT -> {
          dedent += item.text!!
        }
        else -> {
          if (output.trim().isNotEmpty()) {
            return finishCompletion("$output\n", dedent, stateId, idx, params)
          }
        }
      }
    }
    output = output.trimEnd()
    findFirstNonEmptyNewline(output)?.let { index ->
      output = output.substring(0, index)
    }
    return finishCompletion(output, dedent, stateId, null, params)
  }

  fun getAccountStatus(): SupermavenAccountStatus? {
    return agent?.accountStatus
  }

  fun getServiceTier(): String? {
    return agent?.serviceTier
  }

  fun getRepoName(): String? {
    return agent?.repoName
  }

  fun setGitignoreAllowed(allowed: Boolean) {
    val settings = SupermavenSettings.getInstance()
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

  fun documentChanged(path: String) {
    agent?.send(SupermavenInformFileChangedMessage(path))
  }
}